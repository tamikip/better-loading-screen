package com.tamikip.loading;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import java.util.function.Consumer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

@Mod(TeleportLoading.ID)
public final class TeleportLoading {
    public static final String ID = "teleport_loading";
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER.define("enabled", true);
    public static final ModConfigSpec.BooleanValue SOUND = BUILDER.define("loadingSound", true);
    public static final ModConfigSpec.DoubleValue VOLUME = BUILDER.defineInRange("loadingSoundVolume", 0.8, 0.0, 1.0);
    public static final ModConfigSpec.IntValue DISTANCE = BUILDER.comment("Minimum position jump in blocks.").defineInRange("teleportDistance", 24, 8, 1024);
    public static final ModConfigSpec.IntValue RADIUS = BUILDER.comment("Nearby chunk radius; 2 waits for 5 x 5 chunks.").defineInRange("chunkRadius", 2, 0, 8);
    public static final ModConfigSpec.IntValue MINIMUM = BUILDER.defineInRange("minimumDisplayMs", 600, 0, 10000);
    public static final ModConfigSpec.IntValue SETTLE = BUILDER.defineInRange("stableRenderMs", 350, 100, 5000);
    public static final ModConfigSpec.IntValue FADE = BUILDER.defineInRange("fadeOutMs", 800, 100, 5000);
    public static final ModConfigSpec.IntValue TIMEOUT = BUILDER.defineInRange("timeoutMs", 30000, 3000, 120000);
    public static final ModConfigSpec SPEC = BUILDER.build();
    public static Consumer<DefinitionPayload> definitionReceiver = packet -> {};
    private static String serverDefinitions = "[]";
    public TeleportLoading(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
        bus.addListener(this::payloads);
        NeoForge.EVENT_BUS.addListener(this::reload);
        NeoForge.EVENT_BUS.addListener(this::sync);
    }
    private void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(DefinitionPayload.TYPE, DefinitionPayload.CODEC,
            (packet, context) -> context.enqueueWork(() -> definitionReceiver.accept(packet)));
    }
    private void reload(AddReloadListenerEvent event) {
        event.addListener(new DefinitionReload(data -> serverDefinitions = data.toString()));
    }
    private void sync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().filter(player -> player.connection.hasChannel(DefinitionPayload.TYPE))
            .forEach(player -> PacketDistributor.sendToPlayer(player, new DefinitionPayload(serverDefinitions)));
    }
}
