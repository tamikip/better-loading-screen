package com.tamikip.loading;

public final class LoadingStyle {
    public static boolean night(long dayTime) {
        long time = Math.floorMod(dayTime, 24000);
        return time >= 13000 && time < 23000;
    }
    public static int emblem(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> 0;
            case "minecraft:the_nether" -> 1;
            case "minecraft:the_end" -> 2;
            default -> 3;
        };
    }
    public static int background(boolean night) { return night ? 0x1C1B21 : 0xFFFFFF; }
    public static int accent(boolean night) { return night ? 0xCAB881 : 0x899AA5; }
    public static int elements(boolean night) { return night ? 0xECE5D6 : 0x899AA5; }
    public static Layout layout(int width, int height) {
        float reference = Math.min(width, height * 2.24f);
        int emblem = Math.max(1, (int) (reference * 0.14));
        int row = Math.max(7, (int) (reference * 0.174));
        int rowHeight = Math.max(1, row / 3);
        int line = height - (int) (reference * 0.038);
        return new Layout(emblem, height / 2 - (int) (reference * 0.033) - emblem / 2,
            line - (int) (reference * 0.059), line - (int) (reference * 0.0356), row, rowHeight, line - rowHeight / 2, line);
    }
    public record Layout(int emblemSize, int emblemY, int titleY, int bodyY,
                         int rowWidth, int rowHeight, int rowY, int lineY) {}
}
