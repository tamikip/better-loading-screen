package com.tamikip.loading;

public final class LoadingState {
    private long started = -1, stable = -1, fading = -1;
    public void start(long now) { if (active()) return; started = now; stable = fading = -1; }
    public void reset() { started = stable = fading = -1; }
    public boolean active() { return started >= 0; }
    public boolean fading() { return fading >= 0; }
    public boolean timedOut(long now, long timeout) { return active() && now - started >= timeout; }
    public void update(long now, boolean ready, long minimum, long settle, long fade, long timeout) {
        if (!active()) return;
        if (fading >= 0) {
            if (now - fading >= fade) reset();
            return;
        }
        boolean expired = timedOut(now, timeout);
        if (!ready && !expired) { stable = -1; return; }
        if (stable < 0) stable = now;
        if (fading < 0 && (expired || (now - started >= minimum && now - stable >= settle))) fading = now;
        if (fading >= 0 && now - fading >= fade) reset();
    }
    public float progress(long now) {
        if (!active()) return 0;
        return (float) (0.12 + 0.72 * (1 - Math.exp(-Math.max(0, now - started) / 2800.0)));
    }
    public float alpha(long now, long duration) {
        if (!active()) return 0;
        if (fading < 0) return 1;
        double t = Math.clamp((double) (now - fading) / duration, 0, 1);
        return (float) (1 - t * t * (3 - 2 * t));
    }
}
