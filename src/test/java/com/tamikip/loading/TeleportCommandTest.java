package com.tamikip.loading;

public final class TeleportCommandTest {
    public static void main(String[] args) {
        check("tp 100 80 -20", 100, 80, -20, "minecraft:overworld");
        check("minecraft:teleport @s ~10 ~ ~-5", 20, 64, 25, "minecraft:overworld");
        check("teleport Preview 1 2 3 90 0", 1, 2, 3, "minecraft:overworld");
        check("execute in minecraft:the_nether run tp @s 1 90 3", 1, 90, 3, "minecraft:the_nether");
        check("tp ^1 ^2 ^3", 11, 66, 33, "minecraft:overworld");
        for (String command : new String[]{"tp @a 1 2 3", "tp Other 1 2 3", "tp @s Other", "tp NaN 2 3", "tp ^1 ~2 ^3", "give @s stone", "execute as Other run tp @s 1 2 3"}) {
            if (parse(command) != null) throw new AssertionError("Must not predict: " + command);
        }
        System.out.println("TeleportCommandTest passed");
    }
    private static TeleportCommand.Target parse(String s) { return TeleportCommand.parse(s, "Preview", "minecraft:overworld", 10, 64, 30, 0, 0); }
    private static void check(String command, double x, double y, double z, String dimension) {
        var target = parse(command);
        if (target == null || Math.abs(target.x() - x) > 0.00001 || Math.abs(target.y() - y) > 0.00001 || Math.abs(target.z() - z) > 0.00001 || !target.dimension().equals(dimension))
            throw new AssertionError(command + ": " + target);
    }
}
