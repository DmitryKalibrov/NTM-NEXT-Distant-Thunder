package com.hbm.thunder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Config, packet registration and the server side: tell far-away players that something went boom. */
public final class DistantThunder implements ModInitializer {

    public static final String ID = "ntm_distant_thunder";
    public static final Logger LOG = LoggerFactory.getLogger(ID);

    /** Blocks per second. Real air: 343 m/s = 343 blocks/s. */
    public static double soundSpeed = 343;
    /** Blocks per second of the ground tremor that outruns the air blast (nukes and big blasts only). */
    public static double seismicSpeed = 3000;
    /** Metres over which high frequencies fall to 1/e: a rifle still cracks at a few hundred m, thumps at 3 km. */
    public static double highsDistance = 1500;
    /** Hard cap on how far an event is sent (m). Real reach comes from {@link Acoustics}: ~4 km rifle, ~15 km big blast, ~100 km nuke. */
    public static int maxRange = 100_000;
    /** Volume of this addon's far sounds, 0..1. Independent of the Music/Blocks/Players sliders; only Master applies. */
    public static float volume = 1F;
    public static float shake = 1F;
    public static boolean dustRing = true, debris = true;
    // Explosion-Overhaul-style extras
    public static boolean breakGlass = true, caveIns = true, lampFlicker = true, flash = true, deafness = true, environment = true;
    /** Multiplier for the radius in which the shockwave shatters glass. */
    public static float glassRadius = 1F;

    // Game-tick dedupe: a muke also fires an ExplosionVNT on the same spot in the same tick.
    private static long lastTick = Long.MIN_VALUE;
    private static double lastX, lastY, lastZ;
    private static float lastPower;

    @Override
    public void onInitialize() {
        loadConfig();
        PayloadTypeRegistry.clientboundPlay().register(RumblePayload.TYPE, RumblePayload.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(server -> WorldShock.tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> WorldShock.clear());
    }

    /**
     * @param l0   source level, dB SPL at 1 m (see {@link Acoustics})
     * @param near players closer than this already hear NTM's own sound (they still get flash, shake, deafness)
     */
    public static void emit(ServerLevel level, double x, double y, double z, Kind kind, float l0,
                            Identifier sound, double near) {
        float power = l0;
        if (kind != Kind.GUN) {
            long tick = level.getGameTime();
            if (tick - lastTick <= 1 && power <= lastPower
                    && (x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ) < 256) return;
            lastTick = tick;
            lastX = x;
            lastY = y;
            lastZ = z;
            lastPower = power;
        }

        boolean enclosed = !level.canSeeSky(BlockPos.containing(x, y + 1, z));
        if (kind != Kind.GUN) WorldShock.blast(level, x, y, z, kind, l0, enclosed);

        RumblePayload payload = new RumblePayload(x, y, z, (byte) kind.ordinal(), power, sound, (float) near, enclosed);
        double far = Math.min(Acoustics.reach(l0, kind, Acoustics.AUDIBLE), maxRange), far2 = far * far;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(x, y, z) < far2) ServerPlayNetworking.send(p, payload);
        }
    }

    public enum Kind { NUKE, MUKE, EXPLOSION, GUN }

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(ID + ".properties");
    }

    private static void loadConfig() {
        Path file = configFile();
        Properties p = current();
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) { p.load(r); }
            }
            soundSpeed = Double.parseDouble(p.getProperty("soundSpeed").trim());
            seismicSpeed = Double.parseDouble(p.getProperty("seismicSpeed").trim());
            highsDistance = Double.parseDouble(p.getProperty("highsDistance").trim());
            maxRange = Integer.parseInt(p.getProperty("maxRange").trim());
            volume = Float.parseFloat(p.getProperty("volume").trim());
            shake = Float.parseFloat(p.getProperty("shake").trim());
            dustRing = Boolean.parseBoolean(p.getProperty("dustRing").trim());
            debris = Boolean.parseBoolean(p.getProperty("debris").trim());
            breakGlass = Boolean.parseBoolean(p.getProperty("breakGlass").trim());
            caveIns = Boolean.parseBoolean(p.getProperty("caveIns").trim());
            lampFlicker = Boolean.parseBoolean(p.getProperty("lampFlicker").trim());
            flash = Boolean.parseBoolean(p.getProperty("flash").trim());
            deafness = Boolean.parseBoolean(p.getProperty("deafness").trim());
            environment = Boolean.parseBoolean(p.getProperty("environment").trim());
            glassRadius = Float.parseFloat(p.getProperty("glassRadius").trim());
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Bad config " + file, e);
        }
        saveConfig(); // writes any keys the file was missing
    }

    /** Also called by the in-game volume slider. */
    public static void saveConfig() {
        try (Writer w = Files.newBufferedWriter(configFile())) {
            current().store(w, "NTM-NEXT: Distant Thunder. 1 block = 1 m. Speeds in m/s, distances in m (maxRange: server), "
                    + "volume 0..1 (client, also a slider in Music & Sounds), shake is a multiplier");
        } catch (IOException e) {
            LOG.error("Could not save config", e);
        }
    }

    private static Properties current() {
        Properties p = new Properties();
        p.setProperty("soundSpeed", String.valueOf(soundSpeed));
        p.setProperty("seismicSpeed", String.valueOf(seismicSpeed));
        p.setProperty("highsDistance", String.valueOf(highsDistance));
        p.setProperty("maxRange", String.valueOf(maxRange));
        p.setProperty("volume", String.valueOf(volume));
        p.setProperty("shake", String.valueOf(shake));
        p.setProperty("dustRing", String.valueOf(dustRing));
        p.setProperty("debris", String.valueOf(debris));
        p.setProperty("breakGlass", String.valueOf(breakGlass));
        p.setProperty("caveIns", String.valueOf(caveIns));
        p.setProperty("lampFlicker", String.valueOf(lampFlicker));
        p.setProperty("flash", String.valueOf(flash));
        p.setProperty("deafness", String.valueOf(deafness));
        p.setProperty("environment", String.valueOf(environment));
        p.setProperty("glassRadius", String.valueOf(glassRadius));
        return p;
    }
}
