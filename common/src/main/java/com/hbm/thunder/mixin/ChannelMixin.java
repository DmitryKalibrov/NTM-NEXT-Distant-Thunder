package com.hbm.thunder.mixin;

import com.hbm.thunder.client.Efx;
import com.hbm.thunder.client.SourceHolder;
import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
abstract class ChannelMixin implements SourceHolder {

    @Shadow @Final private int source;

    @Override
    public int ntmdt$source() {
        return source;
    }

    /** RETURN, so we land after Sound Physics Remastered has set its own filter on this source. */
    @Inject(method = "play", at = @At("RETURN"))
    private void ntmdt$play(CallbackInfo ci) {
        Efx.onPlay(source);
    }
}
