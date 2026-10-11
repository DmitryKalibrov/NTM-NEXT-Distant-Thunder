package com.hbm.thunder.mixin;

import com.hbm.thunder.client.Efx;
import com.hbm.thunder.client.SoundRules;
import com.hbm.thunder.client.SourceHolder;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Right after vanilla queues the channel setup, queue ours: low-pass / echo for this sound. */
@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {

    @Inject(method = "play",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V",
                    ordinal = 0, shift = At.Shift.AFTER))
    private void ntmdt$filter(SoundInstance instance, CallbackInfoReturnable<?> cir,
                              @Local ChannelAccess.ChannelHandle handle) {
        Efx.Setup setup = SoundRules.setup(instance);
        handle.execute(channel -> Efx.prepare(((SourceHolder) channel).ntmdt$source(), setup));
    }
}
