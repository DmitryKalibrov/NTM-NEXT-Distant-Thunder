package com.hbm.thunder.mixin;

import com.hbm.thunder.DistantThunder;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mini nukes (Fatman launcher, small warheads). */
@Mixin(targets = "com.hbm.explosion.ExplosionNukeSmall")
abstract class MukeMixin {

    private static final Identifier SOUND = Identifier.fromNamespaceAndPath("hbm", "weapon.muke_explosion");

    @Inject(method = "explode", at = @At("HEAD"))
    private static void ntmdt$muke(CallbackInfo ci,
                                   @Local(argsOnly = true) Level level,
                                   @Local(argsOnly = true, ordinal = 0) double x,
                                   @Local(argsOnly = true, ordinal = 1) double y,
                                   @Local(argsOnly = true, ordinal = 2) double z) {
        if (!(level instanceof ServerLevel server)) return;
        // NTM plays it at volume 15 -> heard up to 240 blocks.
        DistantThunder.emit(server, x, y, z, DistantThunder.Kind.MUKE, 205F, SOUND, 240);
    }
}
