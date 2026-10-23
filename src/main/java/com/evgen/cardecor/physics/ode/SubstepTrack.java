package com.evgen.cardecor.physics.ode;

/**
 * 16-element ring buffer for tracking physics sub-step samples (Zero-GC).
 * Allows client renderer to evaluate exact state at T_r = tick + partialTick - 1.0.
 */
public final class SubstepTrack {
    private static final int N = 16;
    private static final int M = N - 1;
    private final double[] t = new double[N];
    private final float[] y = new float[N];
    private int head = -1;
    private int count = 0;

    public void push(double tTicks, float v) {
        head = (head + 1) & M;
        t[head] = tTicks;
        y[head] = v;
        if (count < N) count++;
    }

    public float sample(double tr) {
        if (count == 0) return 0.0f;
        int b = head;
        if (count == 1 || tr >= t[b]) return y[b];
        int a = (b - 1) & M;
        for (int k = 0; k < count - 2 && t[a] > tr; k++) {
            b = a;
            a = (a - 1) & M;
        }
        if (tr <= t[a]) return y[a];
        double f = (tr - t[a]) / (t[b] - t[a]);
        return (float) (y[a] + f * (y[b] - y[a]));
    }
}
