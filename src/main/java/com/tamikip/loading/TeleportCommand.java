package com.tamikip.loading;

public final class TeleportCommand {
    public record Target(String dimension, double x, double y, double z) {}

    public static Target parse(String command, String player, String dimension, double x, double y, double z, float yaw, float pitch) {
        String[] args = command.trim().split("\\s+");
        int start = 0;
        if (args.length > 4 && args[0].equals("execute") && args[1].equals("in") && args[3].equals("run")) {
            if (!args[2].matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) return null;
            dimension = args[2]; start = 4;
        }
        if (args.length <= start || !args[start].matches("(?:minecraft:)?(?:tp|teleport)")) return null;
        start++;
        if (args.length > start && (args[start].equals("@s") || args[start].equals(player))) start++;
        int remaining = args.length - start;
        if (remaining != 3 && remaining != 5) return null;
        try {
            if (remaining == 5) { coordinate(args[start + 3], yaw); coordinate(args[start + 4], pitch); }
            if (args[start].startsWith("^") || args[start + 1].startsWith("^") || args[start + 2].startsWith("^")) {
                double[] local = new double[3];
                for (int i = 0; i < 3; i++) {
                    String s = args[start + i];
                    if (!s.startsWith("^")) return null;
                    local[i] = s.length() == 1 ? 0 : Double.parseDouble(s.substring(1));
                }
                double a = Math.toRadians(yaw + 90), b = Math.toRadians(-pitch);
                double fx = Math.cos(a) * Math.cos(b), fy = Math.sin(b), fz = Math.sin(a) * Math.cos(b);
                double ux = Math.cos(a) * Math.cos(b + Math.PI / 2), uy = Math.sin(b + Math.PI / 2), uz = Math.sin(a) * Math.cos(b + Math.PI / 2);
                x += -(fy * uz - fz * uy) * local[0] + ux * local[1] + fx * local[2];
                y += -(fz * ux - fx * uz) * local[0] + uy * local[1] + fy * local[2];
                z += -(fx * uy - fy * ux) * local[0] + uz * local[1] + fz * local[2];
            } else {
                x = coordinate(args[start], x); y = coordinate(args[start + 1], y); z = coordinate(args[start + 2], z);
            }
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) ? new Target(dimension, x, y, z) : null;
        } catch (NumberFormatException ex) { return null; }
    }
    private static double coordinate(String s, double origin) {
        return s.startsWith("~") ? origin + (s.length() == 1 ? 0 : Double.parseDouble(s.substring(1))) : Double.parseDouble(s);
    }
}
