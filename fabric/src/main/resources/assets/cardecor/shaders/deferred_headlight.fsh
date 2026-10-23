#version 330 core

uniform sampler2D       uDepthSampler;
uniform sampler2D       uSceneSampler;
uniform sampler2D       uCookieLow;
uniform sampler2D       uCookieHigh;
uniform int             uHasCookieLow;
uniform int             uHasCookieHigh;
uniform sampler2DShadow uShadowMap[2];

uniform mat4  uInvMVP;
uniform vec2  uInvScreenSize;
uniform int   uLightCount;

#define MAX_LIGHTS 16
uniform vec3  uLightOrigin[MAX_LIGHTS];
uniform vec3  uLightDir[MAX_LIGHTS];
uniform vec3  uLightColor[MAX_LIGHTS];
uniform vec3  uLightRight[MAX_LIGHTS];
uniform vec3  uLightUp[MAX_LIGHTS];
uniform float uConeAngleCos[MAX_LIGHTS];
uniform float uRange[MAX_LIGHTS];
uniform float uIntensity[MAX_LIGHTS];
uniform int   uCookieType[MAX_LIGHTS]; // 0 = Low Beam (Cutoff), 1 = High Beam, 2 = Taillight/Omni
uniform int   uShadowSlot[MAX_LIGHTS]; // 0 or 1 for front headlights with shadow maps, -1 for unshadowed

uniform mat4  uLightViewProj[2];
uniform float uShadowTanHalfFov[2];
uniform float uShadowTexelSize;
uniform float uShadowProjA;
uniform float uShadowProjB;
uniform float uShadowBiasNormal;
uniform float uShadowBiasSlope;

in  vec2 texCoord;
out vec4 fragColor;

// Bayer 8x8 matrix dithering to eliminate 8-bit color banding
float Bayer2(vec2 c) { c = 0.5 * floor(c); return fract(1.5 * fract(c.y) + c.x); }
float Bayer4(vec2 c) { return 0.25 * Bayer2(0.5 * c) + Bayer2(c); }
float Bayer8(vec2 c) { return 0.25 * Bayer4(0.5 * c) + Bayer2(c); }

vec3 reconstructWorldPos(vec2 uv, float rawDepth) {
    vec4 clipPos = vec4(uv * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
    vec4 worldH  = uInvMVP * clipPos;
    return worldH.xyz / worldH.w;
}

// Compute mathematically exact geometric surface normal using hardware screen-space derivatives.
// Evaluated inside each 2x2 raster quad, this guarantees zero diagonal seam or crease across block faces.
vec3 getGeometricNormal(vec3 pos) {
    vec3 dX = dFdx(pos);
    vec3 dY = dFdy(pos);
    vec3 n = cross(dX, dY);
    float len = length(n);
    if (len < 1e-6) return vec3(0.0, 1.0, 0.0);
    vec3 normal = n / len;
    // Ensure normal points towards camera so surfaces facing the viewer are shaded correctly
    if (dot(normal, -pos) < 0.0) {
        normal = -normal;
    }
    return normal;
}

// Hardware PCF shadow fetch helper compliant with GLSL 330
float fetchShadow(int slot, vec3 coord) {
    if (slot == 0) return texture(uShadowMap[0], coord);
    if (slot == 1) return texture(uShadowMap[1], coord);
    return 1.0;
}

// Ported directly from FlashlightMod: 9-Tap Normal-Offset + Slope-Biased Hardware PCF Shadow Filter
float sampleShadow(vec3 worldPosTrue, vec3 normal, float cosTheta, int slot) {
    if (slot < 0 || slot >= 2) return 1.0;

    float lightZ0   = max((uLightViewProj[slot] * vec4(worldPosTrue, 1.0)).w, 0.1);
    float texelSize = 2.0 * lightZ0 * uShadowTanHalfFov[slot] * uShadowTexelSize;

    float cosC     = max(cosTheta, 0.02);
    float tanTheta = sqrt(max(0.0, 1.0 - cosC * cosC)) / cosC;
    vec3 samplePos = worldPosTrue + normal * (uShadowBiasNormal * texelSize);

    vec4 lightClip = uLightViewProj[slot] * vec4(samplePos, 1.0);
    vec3 proj = lightClip.xyz / lightClip.w;

    if (abs(proj.x) > 1.0 || abs(proj.y) > 1.0 || proj.z > 1.0 || proj.z < -1.0) {
        return 1.0;
    }

    vec3 sc = proj * 0.5 + 0.5;
    float lightZ = max(lightClip.w, 0.1);

    float worldBias = (0.05 + uShadowBiasSlope * min(tanTheta, 6.0)) * texelSize;
    sc.z -= worldBias * uShadowProjA / (lightZ * lightZ);

    vec2 ts = vec2(uShadowTexelSize);
    float s = 0.0;
    s += fetchShadow(slot, vec3(sc.xy + vec2(-1.0, -1.0) * ts, sc.z));
    s += fetchShadow(slot, vec3(sc.xy + vec2( 0.0, -1.0) * ts, sc.z));
    s += fetchShadow(slot, vec3(sc.xy + vec2( 1.0, -1.0) * ts, sc.z));
    s += fetchShadow(slot, vec3(sc.xy + vec2(-1.0,  0.0) * ts, sc.z));
    s += fetchShadow(slot, vec3(sc.xy + vec2( 0.0,  0.0) * ts, sc.z));
    s += fetchShadow(slot, vec3(sc.xy + vec2( 1.0,  0.0) * ts, sc.z));
    s += fetchShadow(slot, vec3(sc.xy + vec2(-1.0,  1.0) * ts, sc.z));
    s += fetchShadow(slot, vec3(sc.xy + vec2( 0.0,  1.0) * ts, sc.z));
    s += fetchShadow(slot, vec3(sc.xy + vec2( 1.0,  1.0) * ts, sc.z));

    float rawShadow = s / 9.0;
    // Ambient shadow floor: entities cast dark, crisp shadows, but not pure pitch-black holes
    return mix(0.18, 1.0, rawShadow);
}

void main() {
    float rawDepth = texture(uDepthSampler, texCoord).r;
    vec4 sceneColor = texture(uSceneSampler, texCoord);

    // If sky or untouched background, pass through scene untouched
    if (rawDepth >= 0.9999 || uLightCount <= 0) {
        fragColor = sceneColor;
        return;
    }

    vec3 worldPos = reconstructWorldPos(texCoord, rawDepth);
    vec3 normal = getGeometricNormal(worldPos);

    // Detect depth discontinuity (silhouette edges between mobs and background)
    float depthDelta = length(vec2(dFdx(rawDepth), dFdy(rawDepth)));
    float isEdge = smoothstep(0.0015, 0.010, depthDelta);

    vec3 lightAccum = vec3(0.0);

    for (int i = 0; i < uLightCount && i < MAX_LIGHTS; i++) {
        vec3 toPixel = worldPos - uLightOrigin[i];
        float dist = length(toPixel);
        if (dist > uRange[i] || dist < 0.05) continue;

        vec3 pixelDir = toPixel / dist;
        float cosPixel = dot(pixelDir, uLightDir[i]);

        float outerCos = uConeAngleCos[i];
        if (cosPixel <= outerCos) continue;

        // Smooth cosine taper with punchy center hotspot from backup
        float spotNorm = clamp((cosPixel - outerCos) / (1.0 - outerCos), 0.0, 1.0);
        float smoothEdge = smoothstep(0.0, 1.0, spotNorm);
        float coreHotspot = pow(spotNorm, 3.2);
        float spotFactor = smoothEdge * 0.40 + coreHotspot * 0.85;

        // Smooth quadratic distance attenuation
        float normDist = clamp(dist / uRange[i], 0.0, 1.0);
        float distFactor = clamp(1.0 - normDist * normDist, 0.0, 1.0);
        float rangeFalloff = distFactor * distFactor;

        // Perspective Cookie Projection (for realistic low/high beam spread)
        vec3 cookie = vec3(1.0);
        if (uCookieType[i] == 0 && uHasCookieLow != 0) {
            vec3 lightRight = uLightRight[i];
            vec3 lightUp    = uLightUp[i];
            float angle = acos(clamp(cosPixel, -1.0, 1.0));
            float maxAngle = acos(clamp(outerCos, -1.0, 1.0));
            float angleRatio = clamp(angle / maxAngle, 0.0, 1.0);
            vec2 disc = vec2(dot(pixelDir, lightRight), dot(pixelDir, lightUp));
            float discLen = length(disc);
            vec2 discDir = (discLen > 1e-5) ? (disc / discLen) : vec2(1.0, 0.0);
            vec2 cookieUV = vec2(0.5 + 0.5 * angleRatio * discDir.x,
                                 0.5 - 0.5 * angleRatio * discDir.y);
            cookie = texture(uCookieLow, cookieUV).rgb;
        } else if (uCookieType[i] == 1 && uHasCookieHigh != 0) {
            vec3 lightRight = uLightRight[i];
            vec3 lightUp    = uLightUp[i];
            float angle = acos(clamp(cosPixel, -1.0, 1.0));
            float maxAngle = acos(clamp(outerCos, -1.0, 1.0));
            float angleRatio = clamp(angle / maxAngle, 0.0, 1.0);
            vec2 disc = vec2(dot(pixelDir, lightRight), dot(pixelDir, lightUp));
            float discLen = length(disc);
            vec2 discDir = (discLen > 1e-5) ? (disc / discLen) : vec2(1.0, 0.0);
            vec2 cookieUV = vec2(0.5 + 0.5 * angleRatio * discDir.x,
                                 0.5 - 0.5 * angleRatio * discDir.y);
            cookie = texture(uCookieHigh, cookieUV).rgb;
        }

        // Half-Lambert diffuse shading: surfaces facing the light get strong illumination,
        // grazing surfaces drop off into natural shadow, creating authentic 3D contrast
        float nDotL = dot(normal, -pixelDir);
        float standardDiffuse = clamp((nDotL + 0.55) / 1.55, 0.22, 1.0);
        float diffuse = mix(standardDiffuse, max(standardDiffuse, 0.85), isEdge);

        // FlashlightMod Dynamic Shadow Map Sampling
        float shadowFactor = 1.0;
        int sSlot = uShadowSlot[i];
        if (sSlot >= 0 && sSlot < 2) {
            shadowFactor = sampleShadow(worldPos, normal, max(nDotL, 0.0), sSlot);
        }

        float lightStrength = uIntensity[i] * 2.5;
        vec3 lightContribution = uLightColor[i] * cookie * (lightStrength * spotFactor * rangeFalloff * shadowFactor * diffuse);

        lightAccum += lightContribution;
    }

    if (dot(lightAccum, vec3(1.0)) < 0.0001) {
        fragColor = sceneColor;
        return;
    }

    // Authentic contrast-rich lighting inspired by backup_headlight_clean:
    // Shadows outside the light beam remain deep and dark (no flat wash/заливка).
    // The surface's nocturnal color is preserved and amplified proportionally to its albedo,
    // keeping brown dirt textures rich and warm, and avoiding chalky white burnout.
    vec3 baseAlbedo = sceneColor.rgb * 2.2 + vec3(0.04);
    vec3 addedLight = baseAlbedo * lightAccum;

    // Smooth Reinhard highlight compression preserving contrast
    vec3 compressedLight = addedLight / (vec3(1.0) + addedLight * 0.35);

    vec3 finalColor = sceneColor.rgb + compressedLight;

    // Bayer 8x8 dithering to eliminate 8-bit color banding on dark surfaces
    finalColor += (Bayer8(gl_FragCoord.xy) - 0.5) / 255.0;

    fragColor = vec4(clamp(finalColor, 0.0, 1.0), sceneColor.a);
}
