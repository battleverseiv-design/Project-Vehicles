package com.evgen.cardecor.physics.ode;

/**
 * Critically damped 2nd order follower for visual smoothing (Zero-GC).
 * Cutoff frequency ~15 Hz, lag ~20 ms, unconditionally stable, zero overshoot.
 */
public final class CritDamp {
    private double x;
    private double v;
    private boolean init = false;

    public void reset() {
        this.init = false;
        this.x = 0.0;
        this.v = 0.0;
    }

    public double step(double target, double omega, double dt) {
        if (!init) {
            this.x = target;
            this.v = 0.0;
            this.init = true;
            return this.x;
        }
        double e = this.x - target;
        double b = this.v + omega * e;
        double ex = Math.exp(-omega * dt);
        this.x = target + (e + b * dt) * ex;
        this.v = (this.v - omega * b * dt) * ex;
        return this.x;
    }

    public double get() {
        return this.x;
    }
}
