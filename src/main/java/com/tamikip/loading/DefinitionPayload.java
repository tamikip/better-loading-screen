package com.tamikip.loading;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DefinitionPayload(String json) implements CustomPacketPayload {
    public static final Type<DefinitionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TeleportLoading.ID, "definitions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DefinitionPayload> CODEC = new StreamCodec<>() {
        @Override public DefinitionPayload decode(RegistryFriendlyByteBuf b) { return new DefinitionPayload(b.readUtf(131072)); }
        @Override public void encode(RegistryFriendlyByteBuf b, DefinitionPayload p) { b.writeUtf(p.json, 131072); }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
