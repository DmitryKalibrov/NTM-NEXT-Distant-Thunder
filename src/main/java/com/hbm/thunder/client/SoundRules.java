package com.hbm.thunder.client;

import com.hbm.thunder.DistantThunder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Decides, on the client thread at play time, what Efx does to a sound. */
public final class SoundRules {

    static final boolean SOUND_PHYSICS = FabricLoader.getInstance().isModLoaded("sound_physics_remastered");

    private SoundRules() {}

    /** Our acoustics (occlusion + room reverb) are on unless Sound Physics already does them. */
    static boolean acoustics() {
        return DistantThunder.environment && !SOUND_PHYSICS;
    }

    public static Efx.Setup setup(SoundInstance instance) {
        if (instance instanceof DistantSound d) return new Efx.Setup(d.gainHF, 1F, acoustics());
        if (!acoustics() || instance.isRelative() || instance.getSource() == SoundSource.MUSIC
                || instance.getAttenuation() == SoundInstance.Attenuation.NONE) return Efx.NONE;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Efx.NONE;
        int walls = occlusion(mc.level, mc.gameRenderer.mainCamera().position(),
                new Vec3(instance.getX(), instance.getY(), instance.getZ()));
        if (walls == 0) return new Efx.Setup(-1F, -1F, true);
        // Each solid block in the way eats highs and some level: muffled through one wall, a thud through three.
        return new Efx.Setup((float) Math.exp(-0.7 * walls), Math.max(0.3F, 1F - 0.12F * walls), true);
    }

    /** Solid blocks on the straight line between listener and source (ponytail: no materials, cap 8). */
    private static int occlusion(ClientLevel level, Vec3 ear, Vec3 src) {
        double dist = ear.distanceTo(src);
        if (dist < 1.5 || dist > 64) return 0;
        Vec3 step = src.subtract(ear).scale(0.5 / dist);
        BlockPos srcBlock = BlockPos.containing(src), last = BlockPos.containing(ear);
        Vec3 p = ear;
        int walls = 0;
        for (double t = 0; t < dist && walls < 8; t += 0.5) {
            p = p.add(step);
            BlockPos b = BlockPos.containing(p);
            if (b.equals(last) || b.equals(srcBlock)) continue;
            last = b;
            BlockState s = level.getBlockState(b);
            if (s.isSolidRender()) walls++;
        }
        return walls;
    }
}
