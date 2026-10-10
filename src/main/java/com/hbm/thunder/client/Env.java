package com.hbm.thunder.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Where the listener is: open sky, under a roof, or deep underground. */
enum Env {
    OPEN, INDOOR, CAVE;

    static Env of(ClientLevel level, Vec3 eye) {
        BlockPos p = BlockPos.containing(eye);
        if (level.canSeeSky(p)) return OPEN;
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, p.getX(), p.getZ());
        return p.getY() < surface - 6 ? CAVE : INDOOR; // ponytail: heightmap heuristic, no raycasts
    }
}
