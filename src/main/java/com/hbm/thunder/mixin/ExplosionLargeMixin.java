package com.hbm.thunder.mixin;

import com.hbm.thunder.Blasts;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ExplosionLarge goes through vanilla Level.explode, which ExplosionVntMixin doesn't see. */
@Mixin(targets = "com.hbm.explosion.ExplosionLarge")
abstract class ExplosionLargeMixin {

    @Inject(method = "explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;DDDFZZZZ)V", at = @At("HEAD"))
    private static void ntmdt$explode(Level level, Entity detonator, double x, double y, double z, float strength,
                                      boolean fire, boolean cloud, boolean rubble, boolean shrapnel, CallbackInfo ci) {
        Blasts.explosion(level, x, y, z, strength);
    }
}
