package com.tamikip.loading;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LoadingDefinition(Map<String, Icon> dimensions, Icon fallback, Icon elements,
                                List<Tip> tips, String font, String sound) {
    public record Icon(String texture, int width, int height, int u, int v, int regionWidth, int regionHeight, int base) {}
    public record Tip(String title, String body, boolean translate) {}

    public static LoadingDefinition defaults() {
        Map<String, Icon> icons = new HashMap<>();
        String[] ids = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};
        for (int i = 0; i < ids.length; i++) icons.put(ids[i], atlas(i));
        List<Tip> tips = new ArrayList<>();
        for (int i = 0; i < 3; i++) tips.add(new Tip("loading.teleport_loading.tip." + i + ".title", "loading.teleport_loading.tip." + i + ".body", true));
        return new LoadingDefinition(Map.copyOf(icons), atlas(3), new Icon("teleport_loading:textures/gui/elements.png", 2172, 724, 0, 0, 2172, 724, 80),
            List.copyOf(tips), "teleport_loading:tips", "teleport_loading:loading");
    }
    private static Icon atlas(int index) {
        return new Icon("teleport_loading:textures/gui/dimensions.png", 1254, 1254, index % 2 * 627, index / 2 * 627, 627, 627, 100);
    }
    public Icon icon(String dimension) { return dimensions.getOrDefault(dimension, fallback); }

    public LoadingDefinition merge(JsonElement input) {
        JsonObject o = input.getAsJsonObject();
        Map<String, Icon> icons = new HashMap<>(dimensions);
        if (o.has("dimensions")) o.getAsJsonObject("dimensions").entrySet().forEach(e -> icons.put(id(e.getKey()), icon(e.getValue())));
        if (icons.size() > 512) throw new IllegalArgumentException("Too many dimensions");
        List<Tip> list = tips;
        if (o.has("tips")) {
            list = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("tips")) {
                JsonObject t = e.getAsJsonObject();
                String title = t.get("title").getAsString(), body = t.get("body").getAsString();
                if (title.length() > 256 || body.length() > 2048) throw new IllegalArgumentException("Tip too long");
                list.add(new Tip(title, body, t.has("translate") && t.get("translate").getAsBoolean()));
            }
            if (list.isEmpty() || list.size() > 256) throw new IllegalArgumentException("Expected 1..256 tips");
        }
        return new LoadingDefinition(Map.copyOf(icons), o.has("fallback") ? icon(o.get("fallback")) : fallback,
            o.has("elements") ? icon(o.get("elements")) : elements, List.copyOf(list),
            o.has("font") ? id(o.get("font").getAsString()) : font, o.has("sound") ? id(o.get("sound").getAsString()) : sound);
    }
    private static Icon icon(JsonElement e) {
        JsonObject o = e.getAsJsonObject();
        int w = number(o, "width", 0), h = number(o, "height", 0), u = number(o, "u", 0), v = number(o, "v", 0);
        int rw = number(o, "region_width", w), rh = number(o, "region_height", h), base = number(o, "base", 255);
        if (w < 1 || h < 1 || w > 16384 || h > 16384 || rw < 1 || rh < 1 || u < 0 || v < 0 || u + rw > w || v + rh > h || base < 1 || base > 255)
            throw new IllegalArgumentException("Invalid icon bounds");
        return new Icon(id(o.get("texture").getAsString()), w, h, u, v, rw, rh, base);
    }
    private static int number(JsonObject o, String key, int fallback) { return o.has(key) ? o.get(key).getAsInt() : fallback; }
    private static String id(String s) {
        if (!s.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || s.contains("..")) throw new IllegalArgumentException("Invalid resource ID: " + s);
        return s;
    }
}
