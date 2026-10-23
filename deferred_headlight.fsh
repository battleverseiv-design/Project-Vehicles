#version 330 core

uniform sampler2D uDepthSampler;
uniform sampler2D uSceneSampler;

uniform mat4  uInvMVP;
uniform vec2  uInvScreenSize;
uniform int   uLightCount;

#define MAX_LIGHTS 16
uniform vec3  uLightOrigin[MAX_LIGHTS];
uniform vec3  uLightDir[MAX_LIGHTS];
uniform vec3  uLightColor[MAX_LIGHTS];
uniform float uConeAngleCos[MAX_LIGHTS];
uniform float uRange[MAX_LIGHTS];
uniform float uIntensity[MAX_LIGHTS];

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

// Reconstruct surface normal from screen-space depth gradient to give 3D volume to mobs and terrain
vec4 reconstructNormalAndEdge(vec2 uv, float centerDepth, vec3 centerPos) {
    vec2 offset = uInvScreenSize;
    float depthR = texture(uDepthSampler, uv + vec2(offset.x, 0.0)).r;
    float depthL = texture(uDepthSampler, uv - vec2(offset.x, 0.0)).r;
    float depthU = texture(uDepthSampler, uv + vec2(0.0, offset.y)).r;
    float depthD = texture(uDepthSampler, uv - vec2(0.0, offset.y)).r;

    vec3 posR = reconstructWorldPos(uv + vec2(offset.x, 0.0), depthR);
    vec3 posL = reconstructWorldPos(uv - vec2(offset.x, 0.0), depthL);
    vec3 posU = reconstructWorldPos(uv + vec2(0.0, offset.y), depthU);
    vec3 posD = reconstructWorldPos(uv - vec2(0.0, offset.y), depthD);

    vec3 dX = (abs(depthR - centerDepth) < abs(depthL - centerDepth)) ? (posR - centerPos) : (centerPos - posL);
    vec3 dY = (abs(depthU - centerDepth) < abs(depthD - centerDepth)) ? (posU - centerPos) : (centerPos - posD);

    vec3 n = cross(dX, dY);
    float len = length(n);
    vec3 normal = (len < 1e-5) ? vec3(0.0, 1.0, 0.0) : (n / len);

    // Compute edge/discontinuity factor for thin geometry (held weapons, items, tools)
    float maxDelta = max(max(abs(depthR - centerDepth), abs(depthL - centerDepth)),
                         max(abs(depthU - centerDepth), abs(depthD - centerDepth)));
    float isEdgeOrThin = smoothstep(0.0008, 0.008, maxDelta);

    return vec4(normal, isEdgeOrThin);
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
    vec4 normData = reconstructNormalAndEdge(texCoord, rawDepth, worldPos);
    vec3 normal = normData.xyz;
    float isEdgeOrThin = normData.w;

    vec3 lightAccum = vec3(0.0);

    for (int i = 0; i < uLightCount && i < MAX_LIGHTS; i++) {
        vec3 toPixel = worldPos - uLightOrigin[i];
        float dist = length(toPixel);
        if (dist > uRange[i] || dist < 0.05) continue;

        vec3 pixelDir = toPixel / dist;
        float cosPixel = dot(pixelDir, uLightDir[i]);

        float outerCos = uConeAngleCos[i];
        if (cosPixel <= outerCos) continue;

        // Perfectly smooth cosine taper from optical axis (1.0) to cone boundary (outerCos)
        float spotNorm = clamp((cosPixel - outerCos) / (1.0 - outerCos), 0.0, 1.0);
        // Zero derivative at boundary via smoothstep eliminates hard circle boundaries
        float smoothEdge = smoothstep(0.0, 1.0, spotNorm);
        // Focused punchy central core hotspot
        float coreHotspot = pow(spotNorm, 3.2);
        float spotFactor = smoothEdge * 0.45 + coreHotspot * 0.85;

        // Smooth quadratic distance attenuation with continuous boundary derivative
        float normDist = clamp(dist / uRange[i], 0.0, 1.0);
        float distFactor = clamp(1.0 - normDist * normDist, 0.0, 1.0);
        float rangeFalloff = distFactor * distFactor;

        // Wrapped diffuse normal shading for realistic 3D volumetric depth
        float nDotL = dot(normal, -pixelDir);
        float standardDiffuse = clamp((nDotL + 0.55) / 1.55, 0.22, 1.0);
        // For thin items held in hand or edge silhouettes, provide diffuse floor so held items are vibrantly illuminated
        float diffuse = mix(standardDiffuse, max(standardDiffuse, 0.85), isEdgeOrThin);

        float lightStrength = uIntensity[i] * 2.2;
        vec3 lightContribution = uLightColor[i] * (lightStrength * spotFactor * rangeFalloff * diffuse);

        lightAccum += lightContribution;
    }

    if (dot(lightAccum, vec3(1.0)) < 0.0001) {
        fragColor = sceneColor;
        return;
    }

    // Contrast-rich physical surface illumination:
    // Scale the true nocturnal hue with sharp contrast and a minimal floor (0.04)
    // to eliminate flat grey fog while cleanly revealing dark terrain
    vec3 baseAlbedo = sceneColor.rgb * 2.2 + vec3(0.04);
    vec3 addedLight = baseAlbedo * lightAccum;

    // Smooth Reinhard highlight compression preserving contrast
    vec3 compressedLight = addedLight / (vec3(1.0) + addedLight * 0.35);

    vec3 finalColor = sceneColor.rgb + compressedLight;

    // Bayer dithering to eliminate 8-bit banding on dark surfaces
    finalColor += (Bayer8(gl_FragCoord.xy) - 0.5) / 255.0;

    fragColor = vec4(clamp(finalColor, 0.0, 1.0), sceneColor.a);
}
