package com.hbm.thunder.client;

import com.hbm.thunder.Acoustics;
import com.hbm.thunder.DistantThunder;
import com.hbm.thunder.DistantThunder.Kind;
import com.hbm.thunder.RumblePayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Client side of a blast, in the order a real observer gets it: the flash (speed of light), the
 * ground tremor (fast, through rock), then the air blast at the speed of sound: the boom, muffled by
 * distance and walls or echoing in a cave, a hard jolt, dust and stones from leaves and ceilings,
 * lamps flickering, and up close, ringing deafness. A dust ring sweeps over the land with it.
 */
public final class DistantThunderClient {

    /** Fires once the front (speed m/s, started at tick start) has reached the player's current position. */
    private record Wave(Vec3 src, long start, double speed, Runnable action) {}

    private record Ring(double x, double y, double z, long start, float l0, Kind kind) {}

    private static final List<Wave> WAVES = new ArrayList<>();
    private static final List<Ring> RINGS = new ArrayList<>();
    private static final RandomSource RAND = RandomSource.create();
    private static long tick;

    /** HUD layer id of the blast flash; loaders register {@link #renderFlash} under it, on top of the HUD. */
    public static final Identifier FLASH_LAYER = Identifier.fromNamespaceAndPath(DistantThunder.ID, "flash");

    private DistantThunderClient() {}

    public static void renderFlash(GuiGraphicsExtractor graphics) {
        Flash.render(graphics);
    }

    private static Vec3 ear() {
        return Minecraft.getInstance().gameRenderer.mainCamera().position();
    }

    /** dB SPL of this event at the player right now. */
    private static double splHere(RumblePayload p, Kind kind) {
        return Acoustics.spl(p.power(), ear().distanceTo(new Vec3(p.x(), p.y(), p.z())), kind);
    }

    /** A rumble packet arrived (on the client thread). */
    public static void receive(RumblePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Kind kind = Kind.values()[p.kind()];
        Vec3 src = new Vec3(p.x(), p.y(), p.z());
        double d = Math.max(1, ear().distanceTo(src));
        double spl = Acoustics.spl(p.power(), d, kind);

        // Inside "near" NTM already plays its own sound; we only add what NTM doesn't do.
        if (d > p.near()) WAVES.add(new Wave(src, tick, DistantThunder.soundSpeed, () -> boom(p, kind, src)));
        if (kind == Kind.GUN) return;

        // Light arrives instantly. NTM flashes the screen for its own mushroom clouds.
        if (DistantThunder.flash && kind != Kind.NUKE) Flash.add(flash(mc, src, d, spl, p.enclosed()));

        // Ground tremor outruns the air blast; felt from heavy blasts only.
        if (kind == Kind.NUKE || spl > Acoustics.SHAKE + 20)
            WAVES.add(new Wave(src, tick, DistantThunder.seismicSpeed, () -> Shake.add(jolt(splHere(p, kind)) * 0.4F, 100)));

        WAVES.add(new Wave(src, tick, DistantThunder.soundSpeed, () -> airBlast(p, kind, src)));

        if (DistantThunder.dustRing && !p.enclosed() && Acoustics.reach(p.power(), kind, Acoustics.DEAFEN) > 20)
            RINGS.add(new Ring(p.x(), p.y(), p.z(), tick, p.power(), kind));
    }

    /** Camera jolt in degrees for a level: felt from ~115 dB, violent past ~170 dB. */
    private static float jolt(double spl) {
        float j = DistantThunder.shake * (float) Mth.clamp((spl - Acoustics.SHAKE) / 8, 0, 7);
        Minecraft mc = Minecraft.getInstance();
        Env env = mc.level == null ? Env.OPEN : Env.of(mc.level, ear());
        return env == Env.OPEN ? j : j * 1.5F; // enclosed spaces rattle harder
    }

    /** The pressure front reaches the player: jolt, debris, lamps, deafness - all from the level here and now. */
    private static void airBlast(RumblePayload p, Kind kind, Vec3 src) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        double spl = splHere(p, kind);
        float jolt = jolt(spl);
        if (jolt > 0.05F) {
            Shake.add(jolt, kind == Kind.NUKE ? 80 : 35);
            if (DistantThunder.debris && jolt > 0.25F) debris(jolt, Env.of(mc.level, ear()) == Env.CAVE);
        }
        if (DistantThunder.deafness && spl > Acoustics.DEAFEN) {
            float s = (float) Math.min(1, (spl - Acoustics.DEAFEN) / 25);
            Deafness.hit(s, 60 + Math.round(340 * s));
        }
        if (DistantThunder.lampFlicker && spl > Acoustics.LAMPS) Lamps.surge(mc.level, ear(), 48, tick);
    }

    /** Brighter when close, strong, in front of you, and not buried. */
    private static float flash(Minecraft mc, Vec3 src, double d, double spl, boolean enclosed) {
        Vec3 look = mc.player.getViewVector(1F);
        double facing = look.dot(src.subtract(ear()).normalize());
        float a = (float) Mth.clamp((spl - 110) / 50, 0, 1) * (float) Mth.clamp((facing + 0.3) / 1.3, 0.1, 1);
        Env env = Env.of(mc.level, ear());
        if (enclosed && d > 24) a *= 0.1F;        // went off underground, behind rock
        if (!enclosed && env == Env.CAVE) a *= 0.1F; // we're underground, it was on the surface
        return a;
    }

    /** The sound arrives: loudness from the level at the listener, muffling from distance and walls. */
    private static void boom(RumblePayload p, Kind kind, Vec3 src) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Vec3 ear = ear();
        double d = Math.max(1, ear.distanceTo(src));
        // Perceived loudness ~ dB above the noise floor: 45 dB barely there, ~115 dB full scale.
        float volume = (float) Math.pow(Mth.clamp((Acoustics.spl(p.power(), d, kind) - Acoustics.AUDIBLE) / 70, 0, 1), 1.5)
                * DistantThunder.volume;
        float gainHF = Mth.clamp((float) Math.exp(-d / DistantThunder.highsDistance), 0.02F, 1F);
        if (DistantThunder.environment) {
            switch (Env.of(mc.level, ear)) {
                case INDOOR -> { gainHF *= 0.35F; volume *= 0.7F; }     // rumble through walls
                case CAVE -> {
                    if (p.enclosed()) { gainHF *= 0.5F; volume *= 0.85F; } // through the tunnels
                    else { gainHF *= 0.15F; volume *= 0.5F; }              // through the rock above
                }
                case OPEN -> { }
            }
        }
        if (volume < 0.01F) return;
        float pitch = kind == Kind.GUN
                ? 1F - 0.15F * (float) Math.min(d / 3_000, 1)
                : 1F - 0.3F * (float) Math.min(d / 30_000, 1); // far blasts drop into a low roll
        // MASTER: only the master slider scales it, so the addon has its own volume (config "volume").
        mc.getSoundManager().play(new DistantSound(p.sound(), SoundSource.MASTER, ear, src, volume, pitch,
                Math.max(gainHF, 0.01F)));
    }

    /** End of every client tick. */
    public static void tick(Minecraft mc) {
        tick++;
        Shake.tick();
        Deafness.tick();
        if (mc.level == null || mc.player == null) {
            WAVES.clear();
            RINGS.clear();
            Lamps.clear();
            return;
        }
        Vec3 ear = ear();
        for (int i = WAVES.size() - 1; i >= 0; i--) {
            Wave w = WAVES.get(i);
            if ((tick - w.start) / 20.0 * w.speed >= ear.distanceTo(w.src)) {
                WAVES.remove(i);
                w.action.run();
            }
        }
        Lamps.tick(mc.level, tick);
        if (SoundRules.acoustics()) Room.tick(mc, mc.level, tick);
        RINGS.removeIf(ring -> !ring(mc.level, mc.player.position(), ring));
    }

    /** Spawns this tick's slice of the dust front near the player. False once it has died out. */
    private static boolean ring(ClientLevel level, Vec3 player, Ring ring) {
        double r = (tick - ring.start) / 20.0 * DistantThunder.soundSpeed;
        if (r < 4) return true;
        double spl = Acoustics.spl(ring.l0, r, ring.kind);
        if (spl < Acoustics.DEAFEN) return false; // front too weak to lift dust
        float strength = (float) ((spl - Acoustics.DEAFEN) / 10 + 0.2);

        double dx = player.x - ring.x, dz = player.z - ring.z;
        double toPlayer = Math.sqrt(dx * dx + dz * dz);
        double view = 160; // ponytail: only the arc within 160 blocks of the player is drawn
        if (Math.abs(r - toPlayer) > view) return r < toPlayer + view;

        double bearing = Math.atan2(dz, dx);
        double half = r <= view ? Math.PI : Math.asin(Math.min(1, view / r)) + 0.05;
        double step = Math.max(2.5 / r, 2 * half / 160); // at most ~160 points per tick
        for (double a = bearing - half; a <= bearing + half; a += step) {
            double x = ring.x + Math.cos(a) * r, z = ring.z + Math.sin(a) * r;
            int bx = Mth.floor(x), bz = Mth.floor(z);
            if (!level.hasChunk(SectionPos.blockToSectionCoord(bx), SectionPos.blockToSectionCoord(bz))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            BlockState ground = level.getBlockState(new BlockPos(bx, y - 1, bz));
            if (ground.isAir() || !ground.getFluidState().isEmpty()) continue;
            double ox = Math.cos(a) * 0.4, oz = Math.sin(a) * 0.4;
            int n = 1 + (int) Math.min(3, strength);
            for (int i = 0; i < n; i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                        x + RAND.nextGaussian(), y + 0.1, z + RAND.nextGaussian(), ox, 0.25 + RAND.nextFloat() * 0.3, oz);
            }
            if (strength > 1 && RAND.nextInt(3) == 0)
                level.addParticle(ParticleTypes.POOF, x, y + 0.5, z, ox * 0.5, 0.05, oz * 0.5);
        }
        return true;
    }

    /** The jolt shakes dust out of leaves and off ceilings; underground, stones rain down too. */
    private static void debris(float jolt, boolean cave) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        BlockPos base = mc.player.blockPosition();
        int tries = (int) Math.min(cave ? 600 : 300, jolt * (cave ? 220 : 100));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < tries; i++) {
            pos.set(base.getX() + RAND.nextInt(17) - 8, base.getY() + RAND.nextInt(10) - 1, base.getZ() + RAND.nextInt(17) - 8);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            if (!level.getBlockState(pos.below()).isAir()) continue;
            boolean leaves = state.is(BlockTags.LEAVES);
            if (!leaves && !state.isSolidRender()) continue;
            double x = pos.getX() + RAND.nextDouble(), y = pos.getY() - 0.05, z = pos.getZ() + RAND.nextDouble();
            for (int k = 0; k < (leaves ? 3 : 1); k++)
                level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), x, y, z, 0, 0, 0);
            if (cave && RAND.nextInt(4) == 0) // pebbles: block chunks dropping off the ceiling
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 0, -0.2, 0);
        }
    }
}
