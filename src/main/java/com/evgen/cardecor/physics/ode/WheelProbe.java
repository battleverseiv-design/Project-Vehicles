package com.evgen.cardecor.physics.ode;

/**
 * Multi-ray circular envelope probe for wheels.
 * Eliminates violent nose-up impulses on voxel steps (0.5 - 1.0 m) by
 * geometric rate limiting and circular arc envelope projection.
 */
public final class WheelProbe {
    public final double r;
    public final double tanMax;
    private final double[] cosT;
    private final double[] sinT;
    public double x;  // Current compression (m)
    public double v;  // Compression velocity (m/s)
    public boolean grounded;
    public int bestRayIndex;

    public WheelProbe(double r, double[] thetaRad) {
        this.r = r;
        this.cosT = new double[thetaRad.length];
        this.sinT = new double[thetaRad.length];
        double m = 0;
        for (int j = 0; j < thetaRad.length; j++) {
            this.cosT[j] = Math.cos(thetaRad[j]);
            this.sinT[j] = Math.sin(thetaRad[j]);
            m = Math.max(m, Math.abs(thetaRad[j]));
        }
        this.tanMax = Math.tan(m);
    }

    public int getRayCount() {
        return cosT.length;
    }

    public double getSinT(int j) {
        return sinT[j];
    }

    public double getCosT(int j) {
        return cosT[j];
    }

    public void reset(double x0) {
        this.x = x0;
        this.v = 0.0;
        this.grounded = false;
        this.bestRayIndex = 0;
    }

    /**
     * @param t array of measured distances from mount along suspension down axis
     * @param lRest suspension rest length
     * @param vH horizontal velocity of wheel hub in wheel plane
     * @param vAxis velocity along suspension axis
     * @param s suspension specification
     * @param dt physics timestep
     */
    public void update(double[] t, double lRest, double vH, double vAxis, SuspSpec s, double dt) {
        double xRaw = -1e9;
        int bestIdx = 0;
        for (int j = 0; j < t.length; j++) {
            double xj = lRest + r * cosT[j] - t[j];
            if (xj > xRaw) {
                xRaw = xj;
                bestIdx = j;
            }
        }
        this.bestRayIndex = bestIdx;

        // Ground contact threshold: wheel touches ground when compression is within droop range
        this.grounded = (xRaw >= -s.xDroop);

        if (this.grounded) {
            // Symmetric rate limiting: protects against 1-block voxel wall impulses,
            // while allowing full realistic suspension travel speed (up to 3.5 m/s)
            // without ratcheting downwards from contact micro-vibration.
            double maxSpeed = (Math.abs(vH) < 0.5) ? 4.0 : Math.max(3.5, Math.abs(vH) * tanMax + Math.abs(vAxis) + 2.5);
            double maxDelta = maxSpeed * dt;
            double delta = xRaw - this.x;
            delta = Math.max(-maxDelta, Math.min(maxDelta, delta));
            double xn = this.x + delta;
            this.v = (xn - this.x) / dt;
            this.x = xn;
        } else {
            // Free extension towards droop limit in air: critically damped harmonic follower
            double w = s.wn;
            double vf = this.v + (-w * w * (this.x + s.xDroop) - 2.0 * w * this.v) * dt;
            double xf = this.x + vf * dt;
            this.v = vf;
            this.x = Math.max(-s.xDroop, xf);
        }
    }
}
