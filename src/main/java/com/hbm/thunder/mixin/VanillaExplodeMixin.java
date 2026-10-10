package com.hbm.thunder.mixin;

import com.hbm.thunder.Blasts;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Plain vanilla explosions set off by NTM-family entities (NTM itself, NTM Air Defense warheads and
 * spent stages): the one ServerLevel.explode every Level.explode overload ends in. Creepers, TNT and
 * the like stay vanilla. ExplosionLarge also ends up here; emit's same-tick dedupe drops the repeat.
 */
@Mixin(ServerLevel.class)
abstract class VanillaExplodeMixin {

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/util/random/WeightedList;Lnet/minecraft/core/Holder;)V",
            at = @At("HEAD"))
    private void ntmdt$explode(Entity source, DamageSource damage, ExplosionDamageCalculator calculator, double x, double y, double z,
                               float radius, boolean fire, Level.ExplosionInteraction interaction, ParticleOptions small,
                               ParticleOptions large, WeightedList<ExplosionParticleInfo> particles, Holder<SoundEvent> sound,
                               CallbackInfo ci) {
        if (source != null && source.getClass().getName().startsWith("com.hbm.")) Blasts.explosion((ServerLevel) (Object) this, x, y, z, radius);
    }
}
