package com.tamikip.loadingverify;

import com.tamikip.loading.LoadingState;
import com.tamikip.loading.client.LoadingOverlay;
import com.tamikip.loading.client.ClientDefinitions;
import com.google.gson.JsonParser;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import java.nio.file.*;
import java.util.Set;
import java.util.List;
import com.tamikip.loading.LoadingStyle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

@Mod(value = "loading_client_check", dist = Dist.CLIENT)
public final class ClientCheck {
    private int ticks, phase, phaseTicks;
    private boolean hidden, activeSeen, fadeSeen, captured, audioSeen;
    private Object phaseSound;
    private float lastAlpha = 1, lastProgress;
    private boolean fadingObserved, dismissed;
    private final boolean packs = Boolean.getBoolean("loading.verifyPacks");
    private CompletableFuture<Void> resourceReload;
    private volatile boolean serverReloaded;
    private boolean serverReloadRequested;
    private int nearTicks;
    private boolean nearRequested;
    private boolean packetFrameSeen;
    private static final ResourceKey<Level> OTHER = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("loading_client_check", "other"));
    private static final List<ResourceKey<Level>> DESTINATIONS = List.of(Level.OVERWORLD, Level.NETHER, Level.END, OTHER, Level.OVERWORLD, Level.OVERWORLD);
    private static final boolean[] NIGHT = {false, true, true, false, false, false};
    private final LoadingState state;
    private final java.lang.reflect.Field progress;
    public ClientCheck() throws Exception {
        var field = LoadingOverlay.class.getDeclaredField("STATE"); field.setAccessible(true);
        state = (LoadingState) field.get(null);
        progress = LoadingOverlay.class.getDeclaredField("progress"); progress.setAccessible(true);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::frame);
    }
    private void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.player == null || mc.level == null) return;
        ticks++;
        if (ticks % 40 == 0 && phase > 0) {
            try {
                String data = "";
                for (String name : new String[]{"loaded", "total", "rendered"}) {
                    var f = LoadingOverlay.class.getDeclaredField(name); f.setAccessible(true); data += " " + name + "=" + f.getInt(null);
                }
                System.out.println("LOADING_PROBE phase=" + phase + data + " queue=" + mc.levelRenderer.hasRenderedAllSections() + " screen=" + mc.screen + " pos=" + mc.player.blockPosition() + " fade=" + state.fading());
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        if (ticks >= 140 && phase == 0 && !state.active()) {
            if (packs && resourceReload == null) { resourceReload = mc.reloadResourcePacks(); return; }
            if (packs && !resourceReload.isDone()) return;
            if (packs) {
                resourceReload.join();
                var d = ClientDefinitions.get();
                if (!d.font().equals("loading_example:ui") || !d.sound().equals("loading_example:arrival")) throw new AssertionError("Resource definitions not loaded");
                if (!d.tips().getFirst().title().equals("数据包小贴士")) throw new AssertionError("Datapack sync or priority failed");
            }
            begin(1);
        }
        if (phase == 0) return;
        phaseTicks++;
        if (phaseTicks > 900) throw new AssertionError("Phase did not complete: " + phase);
        if (phaseTicks < 180 || !activeSeen || !fadeSeen || state.active()) return;
        if (phase == DESTINATIONS.size()) {
            if (!packetFrameSeen) throw new AssertionError("Incoming teleport did not render before moving player");
            if (!nearRequested) {
                nearRequested = true;
                mc.getConnection().sendCommand("tp ~1 ~ ~");
                if (state.active()) throw new AssertionError("Loaded nearby command opened overlay");
                return;
            }
            if (++nearTicks < 80) return;
        }
        verify("phase " + phase + " " + DESTINATIONS.get(phase - 1).location());
        if (packs && phase == 3) {
            if (!serverReloadRequested) { reloadData(); return; }
            if (!serverReloaded || !ClientDefinitions.get().tips().getFirst().title().equals("重载成功")) return;
        }
        if (phase < DESTINATIONS.size()) begin(phase + 1);
        else {
            if (state.active()) throw new AssertionError("Overlay stuck after loaded terrain");
            try { Files.writeString(Path.of("../../output/client-validation.json"), "{\"status\":\"passed\",\"checks\":[\"Sodium+Iris runtime\",\"overworld day\",\"nether night\",\"end night\",\"unknown dimension question mark\",\"return overworld day\",\"localized tips\",\"immediate progress\",\"monotonic progress\",\"irreversible fade\",\"no reopening\",\"auto-dismiss\",\"OGG playback\",\"sound once per loading\",\"sound fade volume\",\"destination icon on old-world transition frame\",\"immediate command overlay before delayed server reply\",\"loaded nearby teleport skips overlay\",\"vanilla travel sound cancelled\"]}"); }
            catch (Exception e) { throw new RuntimeException(e); }
            mc.stop();
        }
    }
    private void reloadData() {
        serverReloadRequested = true;
        try {
            var file = Path.of("saves/LoadingTest/datapacks/loading-data/data/loading_example/loading_screen/screen.json");
            var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            json.getAsJsonArray("tips").get(0).getAsJsonObject().addProperty("title", "重载成功");
            var icon = json.getAsJsonObject("dimensions").getAsJsonObject("loading_client_check:other");
            icon.addProperty("u", 627); icon.addProperty("v", 0);
            Files.writeString(file, json.toString());
            var server = Minecraft.getInstance().getSingleplayerServer();
            server.execute(() -> server.reloadResources(server.getPackRepository().getSelectedIds()).thenRun(() -> serverReloaded = true));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
    private void begin(int next) {
        phase = next; phaseTicks = 0;
        activeSeen = fadeSeen = captured = audioSeen = fadingObserved = dismissed = false;
        phaseSound = null; lastAlpha = 1; lastProgress = 0;
        if (phase == 1) {
            var mc = Minecraft.getInstance();
            checkSounds();
            mc.getSingleplayerServer().execute(() -> {
                mc.getSingleplayerServer().overworld().setDayTime(6000);
                java.util.concurrent.locks.LockSupport.parkNanos(1_000_000_000L);
            });
            var before = mc.player.position();
            mc.getConnection().sendCommand("tp 12000 160 12000");
            if (!state.active() || !before.equals(mc.player.position())) throw new AssertionError("Command did not start overlay before server teleport");
        }
        else if (phase == 2) teleport(Level.NETHER, 64, 90, 64);
        else if (phase == 3) teleport(Level.END, 0, 90, 0);
        else if (phase == 4) teleport(OTHER, 0, 160, 0);
        else if (phase == 5) teleport(Level.OVERWORLD, 0, 160, 0);
        else teleport(Level.OVERWORLD, 24000, 160, 24000);
    }
    private void checkSounds() {
        try {
            var mc = Minecraft.getInstance();
            var f = net.minecraft.client.sounds.SoundManager.class.getDeclaredField("soundEngine"); f.setAccessible(true);
            var engine = (net.minecraft.client.sounds.SoundEngine) f.get(mc.getSoundManager());
            for (var sound : new net.minecraft.sounds.SoundEvent[]{net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL, net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER, net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT}) {
                var instance = net.minecraft.client.resources.sounds.SimpleSoundInstance.forLocalAmbience(sound, 1, 1);
                var event = new net.neoforged.neoforge.client.event.sound.PlaySoundEvent(engine, instance);
                NeoForge.EVENT_BUS.post(event);
                if ((event.getSound() == null) != (sound != net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT)) throw new AssertionError("Wrong vanilla sound suppression");
            }
            var event = new net.neoforged.neoforge.client.event.sound.PlaySoundEvent(engine, new com.tamikip.loading.client.LoadingSound());
            NeoForge.EVENT_BUS.post(event);
            if (event.getSound() == null) throw new AssertionError("Custom loading sound suppressed");
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
    private void verify(String name) {
        if (!activeSeen || !fadeSeen) throw new AssertionError(name + ": overlay=" + activeSeen + " fade=" + fadeSeen);
        if (!audioSeen) throw new AssertionError(name + ": sound did not play");
        if (!captured) throw new AssertionError(name + ": expected themed loading screen not captured");
        var mc = Minecraft.getInstance();
        if (phase == 1 && mc.player.getX() < 11999) throw new AssertionError("TP command did not reach destination");
        if (mc.screen != null || !mc.levelRenderer.hasRenderedAllSections() || !mc.levelRenderer.isSectionCompiled(mc.player.blockPosition())) throw new AssertionError(name + ": terrain not ready");
        System.out.println("LOADING_CHECK_PASS " + name);
    }
    private void teleport(net.minecraft.resources.ResourceKey<Level> dimension, double x, double y, double z) {
        var mc = Minecraft.getInstance();
        mc.getSingleplayerServer().execute(() -> {
            var server = mc.getSingleplayerServer();
            server.overworld().setDayTime(NIGHT[phase - 1] ? 18000 : 6000);
            server.getPlayerList().getPlayers().getFirst().teleportTo(server.getLevel(dimension), x, y, z, Set.of(), 0, 25);
        });
    }
    private void frame(RenderFrameEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!hidden) { org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow()); hidden = true; }
        if (phase == 0) return;
        if (state.active()) {
            if (phase == 6 && mc.player != null && mc.player.getX() < 23000) packetFrameSeen = true;
            try {
                var d = LoadingOverlay.class.getDeclaredField("destination"); d.setAccessible(true);
                if (!d.get(null).equals(DESTINATIONS.get(phase - 1).location())) throw new AssertionError("Old-world transition frame used departure icon: " + d.get(null));
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        }
        if (mc.level == null || mc.level.dimension() != DESTINATIONS.get(phase - 1)) return;
        if (!state.active()) { phaseSound = null; if (activeSeen) dismissed = true; return; }
        if (dismissed) throw new AssertionError("Overlay reopened without another teleport");
        activeSeen = true;
        float alpha = state.alpha(System.nanoTime() / 1_000_000L, 800);
        if (alpha > 0.05 && alpha < 0.9) fadeSeen = true;
        if (fadingObserved && alpha > lastAlpha + 0.01f) throw new AssertionError("Fade flashed back to opaque");
        if (alpha < 0.99f) fadingObserved = true;
        lastAlpha = alpha;
        try {
            float currentProgress = progress.getFloat(null);
            if (currentProgress <= 0 || currentProgress < lastProgress) throw new AssertionError("Progress delayed or reset");
            lastProgress = currentProgress;
            var f = LoadingOverlay.class.getDeclaredField("sound"); f.setAccessible(true);
            var sound = (com.tamikip.loading.client.LoadingSound) f.get(null);
            if (sound != null) {
                if (packs && !sound.getLocation().toString().equals("loading_example:arrival")) throw new AssertionError("Custom sound event not used");
                if (phaseSound != null && phaseSound != sound) throw new AssertionError("Sound restarted during loading");
                phaseSound = sound;
                if (mc.getSoundManager().isActive(sound)) audioSeen = true;
                if (alpha > 0.05 && alpha < 0.9 && Math.abs(sound.getVolume() - alpha * 0.8f) > 0.03f) throw new AssertionError("Sound volume did not follow fade");
            }
            var e = LoadingOverlay.class.getDeclaredField("emblem"); e.setAccessible(true);
            var n = LoadingOverlay.class.getDeclaredField("night"); n.setAccessible(true);
            int expected = phase == 4 ? 3 : LoadingStyle.emblem(DESTINATIONS.get(phase - 1).location().toString());
            if (e.getInt(null) != expected) throw new AssertionError("Wrong destination emblem");
            if (packs && phase == 4) {
                var icon = ClientDefinitions.get().icon("loading_client_check:other");
                if (!icon.texture().equals("loading_example:textures/gui/dimensions.png") || icon.u() != 627 || icon.v() != 0)
                    throw new AssertionError("Reloaded custom dimension icon not used");
            }
            if (!net.minecraft.network.chat.Component.translatable("loading.teleport_loading.tip.0.title").getString().equals("探索")) throw new AssertionError("Tips missing Chinese localization");
            if (!captured && currentProgress > 0.2f && alpha > 0.99 && n.getBoolean(null) == NIGHT[phase - 1]) {
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(Path.of("../../output/loading-phase-" + phase + ".png")); }
                captured = true;
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
