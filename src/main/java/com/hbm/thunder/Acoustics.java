package com.hbm.thunder;

import com.hbm.thunder.DistantThunder.Kind;

/**
 * Outdoor sound propagation, 1 block = 1 m. Level in dB SPL at distance d from a source with
 * level L0 (dB at 1 m): spherical spreading -20*log10(d) plus air/ground absorption alpha dB per km.
 * Low, heavy blasts carry far (alpha small); a rifle crack dies within a few km.
 *
 * <p>Reference points the numbers are tuned to: rifle shot ~150 dB at 1 m, audible to ~4 km;
 * large conventional blast audible ~15 km; Trinity was heard ~160 km away. Windows break around
 * 150 dB (~1 kPa overpressure), pain/temporary deafness from ~140 dB.
 */
public final class Acoustics {

    /** Below this the sound is lost in ambient noise. */
    public static final double AUDIBLE = 45;
    public static final double DEAFEN = 140, GLASS = 150, CAVE_IN = 165, SHAKE = 115, LAMPS = 130;

    private Acoustics() {}

    public static double alpha(Kind kind) {
        return switch (kind) {
            case NUKE -> 1;
            case MUKE -> 2;
            case EXPLOSION -> 3;
            case GUN -> 8;
        };
    }

    public static double spl(double l0, double d, Kind kind) {
        d = Math.max(d, 1);
        return l0 - 20 * Math.log10(d) - alpha(kind) * d / 1000;
    }

    /** Distance at which the level falls to {@code level} dB (spl is monotonic, so bisect). */
    public static double reach(double l0, Kind kind, double level) {
        if (spl(l0, 1, kind) <= level) return 0;
        double lo = 1, hi = 1_000_000;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (spl(l0, mid, kind) > level) lo = mid;
            else hi = mid;
        }
        return lo;
    }

    /**
     * Source level (dB at 1 m) of the hbm sounds that carry far, NaN for the rest: hand guns ~150,
     * turret cannons ~160 (heard ~6 km), rocket motors on launch ~170 (~7 km and more). Louder
     * playback adds 10*log10(volume).
     */
    public static float shotLevel(String namespace, String path, float volume) {
        if (!namespace.equals("hbm")) return Float.NaN;
        float v = 10F * (float) Math.log10(Math.max(volume, 1F));
        if (path.startsWith("weapon.fire.")) return 150F + v;
        if (path.startsWith("turret.") && path.endsWith("_fire")) return 160F + v;
        if (path.equals("weapon.rocket_flame") || path.equals("missile_take_off") || path.equals("entity.soyuz_takeoff")) return 170F + v;
        return Float.NaN;
    }

    /** demo: java -cp ... com.hbm.thunder.Acoustics (run with -ea) */
    public static void main(String[] args) {
        double gun = reach(150, Kind.GUN, AUDIBLE), big = reach(185, Kind.EXPLOSION, AUDIBLE), nuke = reach(250, Kind.NUKE, AUDIBLE);
        assert gun > 3_000 && gun < 6_000 : "rifle audible ~4 km, got " + gun;
        assert big > 10_000 && big < 25_000 : "big blast audible ~15 km, got " + big;
        assert nuke > 80_000 && nuke < 200_000 : "nuke audible ~100 km, got " + nuke;
        assert Math.abs(spl(150, reach(150, Kind.GUN, 90), Kind.GUN) - 90) < 0.01 : "reach inverts spl";
        float cannon = shotLevel("hbm", "turret.howard_fire", 5);
        float launch = shotLevel("hbm", "weapon.rocket_flame", 10);
        assert Float.isNaN(shotLevel("hbm", "turret.howard_reload", 4)) : "reload is no shot";
        assert Float.isNaN(shotLevel("minecraft", "entity.generic.explode", 4)) : "vanilla sounds stay vanilla";
        double c = reach(cannon, Kind.GUN, AUDIBLE), l = reach(launch, Kind.GUN, AUDIBLE);
        assert c > gun && c < 8_000 : "turret cannon audible a bit beyond a rifle, got " + c;
        assert l > c && l < 12_000 : "missile launch audible beyond a cannon, got " + l;
        System.out.printf("gun %.0f m, cannon %.0f m, launch %.0f m, big blast %.0f m, nuke %.0f m%n", gun, c, l, big, nuke);
    }
}
