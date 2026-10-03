package com.tamikip.loading.client;

import com.tamikip.loading.TeleportLoading;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

@EventBusSubscriber(modid = TeleportLoading.ID, value = Dist.CLIENT)
public final class TransitionSound {
    @SubscribeEvent public static void play(PlaySoundEvent event) {
        if (!TeleportLoading.ENABLED.get() || event.getOriginalSound() instanceof LoadingSound) return;
        String id = event.getOriginalSound().getLocation().toString();
        if (id.equals("minecraft:block.portal.travel") || id.equals("minecraft:block.portal.trigger")) event.setSound(null);
    }
}
