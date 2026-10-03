package com.tamikip.loading;

public final class LoadingStyleTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        check(!LoadingStyle.night(0));
        check(!LoadingStyle.night(12999));
        check(LoadingStyle.night(13000));
        check(LoadingStyle.night(22999));
        check(!LoadingStyle.night(23000));
        check(LoadingStyle.night(37000));
        check(LoadingStyle.night(-1001));
        check(LoadingStyle.emblem("minecraft:overworld") == 0);
        check(LoadingStyle.emblem("minecraft:the_nether") == 1);
        check(LoadingStyle.emblem("minecraft:the_end") == 2);
        check(LoadingStyle.emblem("mod:overworld") == 3);
        check(LoadingStyle.emblem("") == 3);
        for (int[] size : new int[][]{{559,250},{800,450},{1720,720},{320,500},{320,180}}) {
            var p = LoadingStyle.layout(size[0], size[1]);
            check(p.emblemY() + p.emblemSize() < p.titleY());
            check(p.titleY() < p.bodyY() && p.bodyY() < p.rowY());
            check(p.rowY() + p.rowHeight() < size[1]);
            check(p.rowWidth() < size[0] / 3);
        }
        check(LoadingStyle.background(false) != LoadingStyle.background(true));
        System.out.println("PASS: day/night boundaries, wrapped time, dimension emblems, responsive reference proportions");
    }
}
