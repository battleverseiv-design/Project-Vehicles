package com.evgen.cardecor.physics.ode;

/**
 * Parametric Suspension & Bump/Droop Stop specification (Zero-GC, 80 Hz).
 * Designed for arbitrary vehicles (mass m, stiffness k, damping c).
 */
public final class SuspSpec {
    static final double G = 9.80665;
    public final double mC;
    public final double k;
    public final double wn;
    public final double cC;
    public final double cR;
    public final double xMax;
    public final double xB;
    public final double dB;
    public final double fRef;
    public final double cBs1;
    public final double xDroop;
    public final double xFade;

    public SuspSpec(double mTotal, double share, double k, double zetaC, double zetaR,
                    double lRest, double lMin, double droopFrac, double hDrop, double dt) {
        this.k = Math.max(1000.0, k);
        this.mC = Math.max(50.0, mTotal * share);
        this.wn = Math.sqrt(this.k / this.mC);
        double cCrit = 2.0 * Math.sqrt(this.k * this.mC);
        this.cC = zetaC * cCrit;
        this.cR = zetaR * cCrit;
        this.xMax = Math.max(0.05, lRest - lMin);
        double eAll  = this.mC * G * (hDrop + this.xMax) - 0.5 * this.k * this.xMax * this.xMax;
        double eStop = Math.max(eAll, 0.3 * this.mC * G * (hDrop + this.xMax));
        double kCap  = 2.25 * this.mC / (dt * dt);
        this.dB   = Math.min(Math.max(0.25 * this.xMax, Math.sqrt(12.0 * eStop / kCap)), 0.6 * this.xMax);
        this.xB   = this.xMax - this.dB;
        this.fRef = Math.min(4.0 * eStop / this.dB, kCap * this.dB / 3.0);
        this.cBs1 = 0.8 * Math.sqrt(3.0 * this.fRef / this.dB * this.mC); // 2 * zeta_bs * sqrt(K_wall * m_c), zeta_bs = 0.4
        this.xDroop = droopFrac * lRest;
        this.xFade  = Math.max(0.001, 0.35 * this.xDroop);
    }

    /**
     * Suspension reaction force on chassis along suspension axis, N (>= 0).
     * @param x current compression (m), from WheelProbe
     * @param v current compression velocity (m/s), from WheelProbe
     * @param mEff effective quarter mass (kg)
     * @param dt physics timestep (s)
     */
    public double force(double x, double v, double mEff, double dt) {
        double f = k * Math.max(x, 0.0);
        double av = Math.sqrt(v * v + 4e-4) - 0.02; // Smooth |v| (C1 continuous)
        f += 0.5 * (cC + cR) * v + 0.5 * (cC - cR) * av; // c_c when v > 0, c_r when v < 0

        // Progressive cubic elastomer bump stop (C2 continuous on x)
        if (x > xB) {
            double u = (x - xB) / dB;
            f += fRef * u * u * u;
            if (v > 0) {
                f += Math.min(cBs1 * u, 0.5 * mEff / dt) * v;
            }
        }

        // Droop window: smootherstep (C2 continuous) to prevent edge clicks on unweighted wheels
        double t = (x + xDroop) / xFade;
        double w = (t >= 1.0) ? 1.0 : ((t <= 0.0) ? 0.0 : t * t * t * (t * (6.0 * t - 15.0) + 10.0));
        return Math.max(f, 0.0) * w;
    }
}
