package com.tamikip.loading.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.tamikip.loading.DefinitionReload;
import com.tamikip.loading.LoadingDefinition;
import com.tamikip.loading.TeleportLoading;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = TeleportLoading.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientDefinitions {
    private static JsonArray local = new JsonArray(), remote = new JsonArray();
    private static volatile LoadingDefinition current = LoadingDefinition.defaults();
    public static LoadingDefinition get() { return current; }
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
        TeleportLoading.definitionReceiver = packet -> {
            try { remote = JsonParser.parseString(packet.json()).getAsJsonArray(); rebuild(); }
            catch (RuntimeException ex) { LogUtils.getLogger().warn("Invalid server loading definitions", ex); remote = new JsonArray(); rebuild(); }
        };
        event.registerReloadListener(new DefinitionReload(data -> { local = data; rebuild(); }));
    }
    private static synchronized void rebuild() {
        LoadingDefinition next = LoadingDefinition.defaults();
        for (var e : local) next = next.merge(e);
        for (var e : remote) next = next.merge(e);
        current = next;
    }
    @EventBusSubscriber(modid = TeleportLoading.ID, value = Dist.CLIENT)
    public static final class Disconnect {
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { remote = new JsonArray(); rebuild(); }
    }
}
