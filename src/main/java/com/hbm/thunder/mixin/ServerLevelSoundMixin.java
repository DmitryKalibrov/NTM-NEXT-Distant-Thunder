package com.hbm.thunder.mixin;

import com.hbm.thunder.Blasts;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NTM guns, turrets and rocket motors play their sound with plain level.playSound (all overloads
 * land here) at volume ~1-10, i.e. vanilla hears it 16-160 blocks out. Real gunfire carries ~1 km,
 * cannon and rocket launches several km. {@link com.hbm.thunder.Acoustics#shotLevel} picks the sounds that count.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelSoundMixin {

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"))
    private void ntmdt$shot(Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source,
                            float volume, float pitch, long seed, CallbackInfo ci) {
        SoundEvent event = sound.value();
        Blasts.shot((ServerLevel) (Object) this, x, y, z, event.location(), volume, event.getRange(volume));
    }
}
