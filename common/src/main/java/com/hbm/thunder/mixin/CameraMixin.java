package com.hbm.thunder.mixin;

import com.hbm.thunder.client.Shake;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blast and tremor jolts: smooth noise on yaw/pitch, after the camera took the player's rotation. */
@Mixin(Camera.class)
abstract class CameraMixin {

    @Shadow private float xRot;
    @Shadow private float yRot;

    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Shadow protected abstract void move(float forwards, float up, float right);

    @Inject(method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = At.Shift.AFTER))
    private void ntmdt$shake(CallbackInfo ci) {
        float amp = Shake.amplitude();
        if (amp <= 0.001F) return;
        double t = System.nanoTime() / 1.0e9;
        float yaw = (float) (Math.sin(t * 23.0) * 0.6 + Math.sin(t * 41.3 + 1.7) * 0.4);
        float pitch = (float) (Math.sin(t * 29.7 + 0.5) * 0.6 + Math.sin(t * 53.1 + 2.9) * 0.4);
        setRotation(yRot + yaw * amp, xRot + pitch * amp);
        // ...and a physical shove of the head, so it reads as a blast, not a wobble.
        float up = (float) Math.sin(t * 31.0 + 0.9), side = (float) Math.sin(t * 37.0 + 2.2);
        move(0, up * amp * 0.025F, side * amp * 0.025F);
    }
}
