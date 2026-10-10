package com.hbm.thunder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class Blasts {

    private static final Identifier SOUND = Identifier.withDefaultNamespace("entity.generic.explode");

    // repeat filter for turret bursts and rocket motors (they replay their sound every tick or two)
    private static Identifier lastId;
    private static long lastShot = Long.MIN_VALUE;
    private static double lastX, lastY, lastZ;

    private Blasts() {}

    /** Conventional NTM blast; NTM's own sound (volume 4) reaches 64 blocks. */
    public static void explosion(Level level, double x, double y, double z, float size) {
        if (!(level instanceof ServerLevel server) || size < 2) return;
        DistantThunder.emit(server, x, y, z, DistantThunder.Kind.EXPLOSION, 165F + 20F * (float) Math.log10(size), SOUND, 64);
    }

    /**
     * A shot or a launch NTM (or an addon) plays with plain level.playSound.
     * @param heard how far vanilla already delivers the sound itself
     */
    public static void shot(ServerLevel level, double x, double y, double z, Identifier id, float volume, float heard) {
        float l0 = Acoustics.shotLevel(id.getNamespace(), id.getPath(), volume);
        if (Float.isNaN(l0)) return;
        if (!id.getPath().startsWith("weapon.fire.")) {
            long t = level.getGameTime();
            double d2 = (x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ);
            if (id.equals(lastId) && t - lastShot < 3 && d2 < 256) return;
            lastId = id;
            lastShot = t;
            lastX = x;
            lastY = y;
            lastZ = z;
        }
        DistantThunder.emit(level, x, y, z, DistantThunder.Kind.GUN, l0, id, heard);
    }
}
