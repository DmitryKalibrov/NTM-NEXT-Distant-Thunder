package com.hbm.thunder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class Blasts {

    /** NTM's own far-explosion samples: a muffled distant boom, not a close crack. */
    private static final Identifier FAR_SMALL = Identifier.fromNamespaceAndPath("hbm", "weapon.explosion_small_far"),
            FAR_LARGE = Identifier.fromNamespaceAndPath("hbm", "weapon.explosion_large_far");
    /** NTM Air Defense intercept samples; the client swaps mid for far past {@link #INTERCEPT_FAR_AT}. */
    public static final Identifier INTERCEPT_MID = Identifier.fromNamespaceAndPath("ntm_air_defense", "sam.intercept_mid"),
            INTERCEPT_FAR = Identifier.fromNamespaceAndPath("ntm_air_defense", "sam.intercept_far");
    public static final double INTERCEPT_FAR_AT = 1500;

    // repeat filter for turret bursts and rocket motors (they replay their sound every tick or two)
    private static Identifier lastId;
    private static long lastShot = Long.MIN_VALUE;
    private static double lastX, lastY, lastZ;

    private Blasts() {}

    /** Conventional NTM blast; NTM's own sound (volume 4) reaches 64 blocks. */
    public static void explosion(Level level, double x, double y, double z, float size) {
        blast(level, x, y, z, size, 165F);
    }

    /**
     * A small vanilla explosion set off by an NTM-family entity: SAM warheads, falling spent stages.
     * Charges of a few kg, not bombs: 10 dB below a conventional blast of the same size.
     */
    public static void warhead(Level level, double x, double y, double z, float size) {
        blast(level, x, y, z, size, 155F);
    }

    /** NTM Air Defense SAM warhead: like {@link #warhead}, with its own samples. It plays the close one itself (64 blocks). */
    public static void intercept(ServerLevel level, double x, double y, double z, float size) {
        DistantThunder.emit(level, x, y, z, DistantThunder.Kind.EXPLOSION, 155F + 20F * (float) Math.log10(size), INTERCEPT_MID, 64);
    }

    private static void blast(Level level, double x, double y, double z, float size, float base) {
        if (!(level instanceof ServerLevel server) || size < 2) return;
        DistantThunder.emit(server, x, y, z, DistantThunder.Kind.EXPLOSION, base + 20F * (float) Math.log10(size),
                size >= 8 ? FAR_LARGE : FAR_SMALL, 64);
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
