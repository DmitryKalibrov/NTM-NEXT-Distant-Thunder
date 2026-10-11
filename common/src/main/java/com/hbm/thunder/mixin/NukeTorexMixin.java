package com.hbm.thunder.mixin;

import com.hbm.thunder.DistantThunder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every NTM mushroom cloud (nukes, balefire, FLEIJA) is spawned through statFac. */
@Mixin(targets = "com.hbm.entity.effect.EntityNukeTorex")
abstract class NukeTorexMixin {

    private static final Identifier SOUND = Identifier.fromNamespaceAndPath("hbm", "nuclear_explosion");

    @Inject(method = "statFac", at = @At("RETURN"))
    private static void ntmdt$nuke(Level level, double x, double y, double z, float scale, CallbackInfoReturnable<?> cir) {
        if (!(level instanceof ServerLevel server)) return;
        float s = Math.clamp(scale * 0.01F, 0.25F, 5F); // same clamp NTM applies
        // NTM plays its own sound to players the shockwave reaches in its first 150 ticks (~340 blocks).
        DistantThunder.emit(server, x, y, z, DistantThunder.Kind.NUKE, 250F + 20F * (float) Math.log10(s), SOUND, 340);
    }
}
