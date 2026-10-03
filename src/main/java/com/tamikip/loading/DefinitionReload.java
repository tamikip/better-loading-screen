package com.tamikip.loading;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.Map;
import java.util.function.Consumer;

public final class DefinitionReload extends SimpleJsonResourceReloadListener {
    private final Consumer<JsonArray> receiver;
    public DefinitionReload(Consumer<JsonArray> receiver) { super(new Gson(), "loading_screen"); this.receiver = receiver; }
    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        JsonArray accepted = new JsonArray();
        LoadingDefinition checked = LoadingDefinition.defaults();
        for (var entry : files.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            try {
                var candidate = checked.merge(entry.getValue());
                accepted.add(entry.getValue());
                if (accepted.toString().length() > 131072) { accepted.remove(accepted.size() - 1); throw new IllegalArgumentException("Definition sync exceeds 128 KiB"); }
                checked = candidate;
            } catch (RuntimeException ex) { LogUtils.getLogger().warn("Ignoring loading screen definition {}: {}", entry.getKey(), ex.getMessage()); }
        }
        receiver.accept(accepted);
    }
}
