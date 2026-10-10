package com.hbm.thunder.mixin;

import com.hbm.thunder.DistantThunder;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NTM guns play their shot with plain level.playSound (all overloads land here) using
 * hbm:weapon.fire.* at volume ~1, i.e. vanilla hears it 16 blocks out. Real gunfire carries ~1 km.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelSoundMixin {

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"))
    private void ntmdt$shot(Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source,
                            float volume, float pitch, long seed, CallbackInfo ci) {
        Identifier id = sound.value().location();
        if (!id.getNamespace().equals("hbm") || !id.getPath().startsWith("weapon.fire.")) return;
        float heard = sound.value().getRange(volume); // vanilla already delivers it this far
        DistantThunder.emit((ServerLevel) (Object) this, x, y, z, DistantThunder.Kind.GUN,
                150F + 10F * (float) Math.log10(Math.max(volume, 1F)), id, heard);
    }
}
