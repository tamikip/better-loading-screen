package com.tamikip.loading;

public final class LoadingStateTest {
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        LoadingState s = new LoadingState();
        s.start(0);
        s.update(300, true, 600, 250, 800, 30000);
        check(s.alpha(300, 800) == 1);
        s.update(500, false, 600, 250, 800, 30000);
        s.update(650, true, 600, 250, 800, 30000);
        s.update(850, true, 600, 250, 800, 30000);
        check(s.alpha(850, 800) == 1);
        s.update(950, true, 600, 250, 800, 30000);
        check(s.alpha(1350, 800) > 0.4 && s.alpha(1350, 800) < 0.6);
        s.update(1100, false, 600, 250, 800, 30000);
        check(s.alpha(1100, 800) < 1);
        float previous = s.alpha(1100, 800);
        for (long now = 1150; now < 1750; now += 50) {
            s.update(now, now % 100 == 0, 600, 250, 800, 30000);
            check(s.alpha(now, 800) <= previous);
            previous = s.alpha(now, 800);
        }
        s.update(1751, false, 600, 250, 800, 30000);
        check(!s.active());
        s.start(1200);
        s.update(31200, false, 600, 250, 800, 30000);
        s.update(32001, false, 600, 250, 800, 30000);
        check(!s.active());
        s.start(40000);
        check(s.progress(40000) > 0);
        check(s.progress(60000) <= 0.84f);
        float initial = s.progress(41000);
        s.start(41000);
        check(s.progress(41000) == initial);
        s.reset();
        check(!s.active());
        System.out.println("PASS: minimum hold, readiness debounce, irreversible fade, immediate progress, duplicate start, timeout, disconnect");
    }
}
