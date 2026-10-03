package com.tamikip.loading.client;

import com.tamikip.loading.TeleportLoading;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class LoadingSound extends AbstractTickableSoundInstance {
    public LoadingSound() {
        super(SoundEvent.createVariableRangeEvent(ResourceLocation.parse(ClientDefinitions.get().sound())),
            SoundSource.MASTER, SoundInstance.createUnseededRandom());
        relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
        volume = TeleportLoading.VOLUME.get().floatValue();
    }
    public void fade(float alpha) { volume = alpha * TeleportLoading.VOLUME.get().floatValue(); }
    @Override public void tick() {}
}
