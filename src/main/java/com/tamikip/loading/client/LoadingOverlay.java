package com.tamikip.loading.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tamikip.loading.LoadingState;
import com.tamikip.loading.LoadingStyle;
import com.tamikip.loading.LoadingDefinition;
import com.tamikip.loading.TeleportCommand;
import com.tamikip.loading.mixin.MinecraftInvoker;
import com.tamikip.loading.TeleportLoading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

public final class LoadingOverlay {
    private static final LoadingState STATE = new LoadingState();
    private static ClientLevel previousLevel;
    private static Vec3 previousPosition;
    private static boolean receiving, suppressed;
    private static int loaded, total, rendered, sections;
    private static float progress;
    private static long finishStart = -1;
    private static float finishFrom;
    private static LoadingSound sound;
    private static boolean soundStarted;
    private static int emblem = 3, tip = -1;
    private static boolean night;
    private static ResourceLocation destination = ResourceLocation.withDefaultNamespace("unknown");
    private static long commandPendingUntil = -1;
    private static boolean preparingPosition;

    private static long now() { return System.nanoTime() / 1_000_000L; }

    public static void dimension(Minecraft mc, ResourceLocation target) {
        if (!TeleportLoading.ENABLED.get() || mc.level == null || target.equals(mc.level.dimension().location())) return;
        destination = target;
        emblem = LoadingStyle.emblem(target.toString());
        suppressed = false;
        commandPendingUntil = -1;
        start(now());
    }

    public static void position(Minecraft mc, ClientboundPlayerPositionPacket packet) {
        if (!TeleportLoading.ENABLED.get() || mc.level == null || mc.player == null) return;
        var relative = packet.getRelativeArguments();
        Vec3 target = new Vec3(packet.getX() + (relative.contains(RelativeMovement.X) ? mc.player.getX() : 0),
            packet.getY() + (relative.contains(RelativeMovement.Y) ? mc.player.getY() : 0),
            packet.getZ() + (relative.contains(RelativeMovement.Z) ? mc.player.getZ() : 0));
        commandPendingUntil = -1;
        destination = mc.level.dimension().location();
        if (!STATE.active() && target.distanceToSqr(mc.player.position()) >= Math.pow(TeleportLoading.DISTANCE.get(), 2)
            && !probe(mc, BlockPos.containing(target))) {
            suppressed = false;
            start(now());
            preparingPosition = true;
            try { ((MinecraftInvoker) mc).teleportLoading$renderFrame(false); }
            finally { preparingPosition = false; }
        }
    }

    public static void command(Minecraft mc, String command) {
        if (!TeleportLoading.ENABLED.get() || mc.level == null || mc.player == null || STATE.active()) return;
        var target = TeleportCommand.parse(command, mc.player.getGameProfile().getName(), mc.level.dimension().location().toString(),
            mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot());
        if (target == null) return;
        boolean changed = !target.dimension().equals(mc.level.dimension().location().toString());
        Vec3 position = new Vec3(target.x(), target.y(), target.z());
        if (!changed && (position.distanceToSqr(mc.player.position()) < Math.pow(TeleportLoading.DISTANCE.get(), 2) || probe(mc, BlockPos.containing(position)))) return;
        destination = ResourceLocation.parse(target.dimension());
        emblem = LoadingStyle.emblem(target.dimension());
        suppressed = false;
        long time = now();
        commandPendingUntil = time + 5000;
        start(time);
    }

    public static void render(Minecraft mc) {
        if (!TeleportLoading.ENABLED.get() || mc.getConnection() == null) { reset(); return; }
        boolean loadingScreen = mc.screen instanceof ReceivingLevelScreen;
        long time = now();
        if (loadingScreen && !receiving) {
            if (!STATE.active() && mc.level != null) destination = mc.level.dimension().location();
            start(time); suppressed = false;
        }
        receiving = loadingScreen;
        if (mc.level != null && mc.player != null) {
            if (!STATE.active()) destination = mc.level.dimension().location();
            emblem = LoadingStyle.emblem(destination.toString());
            if (destination.equals(mc.level.dimension().location())) night = LoadingStyle.night(mc.level.getDayTime());
            Vec3 position = mc.player.position();
            boolean changed = previousLevel != mc.level;
            int threshold = TeleportLoading.DISTANCE.get();
            boolean jumped = previousPosition != null && position.distanceToSqr(previousPosition) >= threshold * threshold;
            if (changed || jumped) suppressed = false;
            boolean ready = !preparingPosition && time >= commandPendingUntil && destination.equals(mc.level.dimension().location()) && (STATE.active() || changed || jumped) && probe(mc);
            if (commandPendingUntil >= 0 && time >= commandPendingUntil) {
                commandPendingUntil = -1;
                destination = mc.level.dimension().location();
            }
            if (!suppressed && (changed || jumped) && !ready) start(time);
            previousLevel = mc.level;
            previousPosition = position;
            if (STATE.timedOut(time, TeleportLoading.TIMEOUT.get())) {
                suppressed = true;
                if (loadingScreen) mc.screen.onClose();
            }
            STATE.update(time, ready && !(mc.screen instanceof ReceivingLevelScreen),
                TeleportLoading.MINIMUM.get(), TeleportLoading.SETTLE.get(), TeleportLoading.FADE.get(), TeleportLoading.TIMEOUT.get());
        } else if (!loadingScreen) {
            reset();
            return;
        } else {
            STATE.update(time, false, TeleportLoading.MINIMUM.get(), TeleportLoading.SETTLE.get(), TeleportLoading.FADE.get(), TeleportLoading.TIMEOUT.get());
        }
        if (!STATE.active()) { stopSound(); return; }
        if (TeleportLoading.SOUND.get() && mc.level != null && destination.equals(mc.level.dimension().location())) {
            if (!soundStarted) {
                sound = new LoadingSound();
                mc.getSoundManager().play(sound);
                soundStarted = true;
            }
            if (sound != null) sound.fade(STATE.alpha(time, TeleportLoading.FADE.get()));
        } else if (sound != null) {
            mc.getSoundManager().stop(sound);
            sound = null;
        }
        if (STATE.fading()) {
            if (finishStart < 0) { finishStart = time; finishFrom = progress; }
            progress = finishFrom + (1 - finishFrom) * Math.clamp((time - finishStart) / 250f, 0, 1);
        } else {
            finishStart = -1;
            progress = Math.max(progress, STATE.progress(time));
        }
        GuiGraphics gui = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        draw(mc, gui, time);
        gui.flush();
    }

    private static boolean probe(Minecraft mc) {
        return probe(mc, mc.player.blockPosition());
    }

    private static boolean probe(Minecraft mc, BlockPos center) {
        // Loaded data is independent of visible section meshes (occluded sections need not compile).
        int radius = Math.min(TeleportLoading.RADIUS.get(), Math.max(0, mc.options.renderDistance().get() - 1));
        int cx = center.getX() >> 4, cz = center.getZ() >> 4;
        loaded = rendered = sections = 0;
        total = (radius * 2 + 1) * (radius * 2 + 1);
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                var chunk = mc.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk != null) loaded++;
            }
        }
        boolean centerCompiled = mc.level.isOutsideBuildHeight(center) || mc.levelRenderer.isSectionCompiled(center);
        sections = 1;
        rendered = centerCompiled ? 1 : 0;
        return loaded == total && centerCompiled && mc.levelRenderer.hasRenderedAllSections();
    }

    private static void draw(Minecraft mc, GuiGraphics gui, long time) {
        float alpha = STATE.alpha(time, TeleportLoading.FADE.get());
        int a = Math.clamp(Math.round(alpha * 255), 0, 255);
        if (a < 1) return;
        int w = gui.guiWidth(), h = gui.guiHeight();
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 1000);
        gui.fill(0, 0, w, h, (a << 24) | LoadingStyle.background(night));
        var layout = LoadingStyle.layout(w, h);
        var definition = ClientDefinitions.get();
        var icon = definition.icon(destination.toString());
        var elements = definition.elements();
        int rowWidth = layout.rowWidth(), rowHeight = layout.rowHeight();
        int x = (w - rowWidth) / 2, y = layout.rowY();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        tint(LoadingStyle.accent(night), alpha, icon.base());
        blit(gui, icon, (w - layout.emblemSize()) / 2, layout.emblemY(), layout.emblemSize(), layout.emblemSize());
        gui.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        int lineColor = (Math.round(a * 0.24f) << 24) | LoadingStyle.accent(night);
        int gap = Math.max(8, h / 36);
        gui.fill(0, layout.lineY(), x - gap, layout.lineY() + 1, lineColor);
        gui.fill(x + rowWidth + gap, layout.lineY(), w, layout.lineY() + 1, lineColor);
        tint(LoadingStyle.elements(night), alpha * 0.18f, elements.base());
        blit(gui, elements, x, y, rowWidth, rowHeight);
        gui.flush();
        gui.enableScissor(x, y, x + Math.round(rowWidth * progress), y + rowHeight);
        tint(LoadingStyle.elements(night), alpha, elements.base());
        blit(gui, elements, x, y, rowWidth, rowHeight);
        gui.flush();
        gui.disableScissor();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (a >= 4) {
            int color = (a << 24) | LoadingStyle.accent(night);
            var selected = definition.tips().get(Math.floorMod(tip, definition.tips().size()));
            text(mc, gui, selected.translate() ? Component.translatable(selected.title()) : Component.literal(selected.title()),
                w / 2, layout.titleY(), color, w);
            text(mc, gui, selected.translate() ? Component.translatable(selected.body()) : Component.literal(selected.body()),
                w / 2, layout.bodyY(), color, (int) (w * 0.74));
        }
        gui.flush();
        gui.pose().popPose();
    }

    private static void tint(int rgb, float alpha, float base) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((rgb >> 16) & 255) / base, ((rgb >> 8) & 255) / base, (rgb & 255) / base, alpha);
    }

    private static void blit(GuiGraphics gui, LoadingDefinition.Icon icon, int x, int y, int width, int height) {
        gui.blit(ResourceLocation.parse(icon.texture()), x, y, width, height, icon.u(), icon.v(),
            icon.regionWidth(), icon.regionHeight(), icon.width(), icon.height());
    }

    private static void text(Minecraft mc, GuiGraphics gui, Component text, int center, int y, int color, int width) {
        var lines = mc.font.split(text.copy().withStyle(Style.EMPTY.withFont(ResourceLocation.parse(ClientDefinitions.get().font()))), width);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            var line = lines.get(i);
            gui.drawString(mc.font, line, center - mc.font.width(line) / 2, y + i * mc.font.lineHeight, color, false);
        }
        gui.flush();
    }

    private static void start(long time) {
        if (STATE.active()) return;
        tip = Math.floorMod(tip + 1, ClientDefinitions.get().tips().size());
        STATE.start(time); progress = STATE.progress(time); finishStart = -1;
    }

    private static void reset() {
        stopSound();
        STATE.reset();
        previousLevel = null;
        previousPosition = null;
        receiving = suppressed = false;
        loaded = total = rendered = sections = 0;
        emblem = 3;
        night = false;
        destination = ResourceLocation.withDefaultNamespace("unknown");
        commandPendingUntil = -1;
    }

    private static void stopSound() {
        if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound);
        sound = null;
        soundStarted = false;
    }
}
