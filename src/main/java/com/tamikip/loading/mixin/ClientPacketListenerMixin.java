package com.tamikip.loading.mixin;

import com.tamikip.loading.client.LoadingOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V", shift = At.Shift.AFTER))
    private void teleportLoading$dimension(ClientboundRespawnPacket packet, CallbackInfo ci) {
        LoadingOverlay.dimension(Minecraft.getInstance(), packet.commonPlayerSpawnInfo().dimension().location());
    }
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V", shift = At.Shift.AFTER))
    private void teleportLoading$position(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        LoadingOverlay.position(Minecraft.getInstance(), packet);
    }
    @Inject(method = "sendCommand", at = @At("TAIL"))
    private void teleportLoading$command(String command, CallbackInfo ci) {
        LoadingOverlay.command(Minecraft.getInstance(), command);
    }
}
