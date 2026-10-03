package com.tamikip.loading;

import com.google.gson.JsonParser;

public final class LoadingDefinitionTest {
    public static void main(String[] args) {
        var defaults = LoadingDefinition.defaults();
        check(defaults.tips().size() == 3, "default tips");
        var custom = defaults.merge(JsonParser.parseString("""
            {"dimensions":{"example:moon":{"texture":"example:textures/moon.png","width":128,"height":128,"base":255}},
             "tips":[{"title":"Moon","body":"Custom text"}],"font":"example:ui","sound":"example:arrival"}
            """));
        check(custom.icon("example:moon").texture().equals("example:textures/moon.png"), "new dimension");
        check(custom.icon("minecraft:overworld").equals(defaults.icon("minecraft:overworld")), "merge preserves other dimensions");
        check(custom.icon("unknown:void").equals(defaults.icon("unknown:void")), "fallback");
        check(custom.tips().size() == 1 && custom.tips().getFirst().body().equals("Custom text"), "replace tips");
        check(custom.font().equals("example:ui") && custom.sound().equals("example:arrival"), "font and sound");
        check(defaults.tips().size() == 3, "immutable merge");
        reject(defaults, "{\"tips\":[]}");
        reject(defaults, "{\"dimensions\":{\"BAD ID\":{}}}");
        reject(defaults, "{\"elements\":{\"texture\":\"x:y\",\"width\":0,\"height\":1}}");
        reject(defaults, "{\"font\":\"../escape\"}");
        System.out.println("LoadingDefinitionTest passed");
    }
    private static void reject(LoadingDefinition d, String json) {
        try { d.merge(JsonParser.parseString(json)); } catch (RuntimeException expected) { return; }
        throw new AssertionError("Invalid definition accepted: " + json);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
