package com.hbm.thunder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class Blasts {

    private static final Identifier SOUND = Identifier.withDefaultNamespace("entity.generic.explode");

    private Blasts() {}

    /** Conventional NTM blast; NTM's own sound (volume 4) reaches 64 blocks. */
    public static void explosion(Level level, double x, double y, double z, float size) {
        if (!(level instanceof ServerLevel server) || size < 2) return;
        DistantThunder.emit(server, x, y, z, DistantThunder.Kind.EXPLOSION, 165F + 20F * (float) Math.log10(size), SOUND, 64);
    }
}
