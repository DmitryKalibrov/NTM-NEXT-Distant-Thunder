package com.hbm.thunder.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/**
 * Power surge: when the blast front reaches the player, redstone lamps around them flicker and spark for a moment. Purely client-side;
 * the original block state is put back afterwards, circuits are never touched.
 */
final class Lamps {

    private record Flicker(ClientLevel level, BlockPos pos, BlockState original, long end) {}

    private static final List<Flicker> ACTIVE = new ArrayList<>();
    private static final RandomSource RAND = RandomSource.create();
    private static final int MAX = 48;

    private Lamps() {}

    /** Lamps within r of center (the player: the surge is felt where the pressure front is). */
    static void surge(ClientLevel level, Vec3 src, double r, long now) {
        double r2 = r * r;
        int minCx = SectionPos.blockToSectionCoord(Mth.floor(src.x - r)), maxCx = SectionPos.blockToSectionCoord(Mth.floor(src.x + r));
        int minCz = SectionPos.blockToSectionCoord(Mth.floor(src.z - r)), maxCz = SectionPos.blockToSectionCoord(Mth.floor(src.z + r));
        int minSy = Math.max(level.getMinSectionY(), SectionPos.blockToSectionCoord(Mth.floor(src.y - r)));
        int maxSy = Math.min(level.getMaxSectionY(), SectionPos.blockToSectionCoord(Mth.floor(src.y + r)));
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int sy = minSy; sy <= maxSy; sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir() || !section.maybeHas(s -> s.is(Blocks.REDSTONE_LAMP))) continue;
                    for (int i = 0; i < 4096; i++) {
                        int lx = i & 15, ly = (i >> 8) & 15, lz = (i >> 4) & 15;
                        BlockState s = section.getBlockState(lx, ly, lz);
                        if (!s.is(Blocks.REDSTONE_LAMP)) continue;
                        BlockPos pos = new BlockPos((cx << 4) + lx, (sy << 4) + ly, (cz << 4) + lz);
                        if (pos.distToCenterSqr(src) > r2 || busy(pos)) continue;
                        ACTIVE.add(new Flicker(level, pos, s, now + 20 + RAND.nextInt(25)));
                        if (ACTIVE.size() >= MAX) return;
                    }
                }
            }
        }
    }

    private static boolean busy(BlockPos pos) {
        for (Flicker f : ACTIVE) if (f.pos.equals(pos)) return true;
        return false;
    }

    static void tick(ClientLevel level, long now) {
        ACTIVE.removeIf(f -> {
            if (f.level != level) return true;
            BlockState cur = level.getBlockState(f.pos);
            if (!cur.is(Blocks.REDSTONE_LAMP)) return true;
            if (now >= f.end) {
                level.setBlock(f.pos, f.original, 3);
                return true;
            }
            if (RAND.nextInt(3) == 0) {
                boolean lit = !cur.getValue(RedstoneLampBlock.LIT);
                level.setBlock(f.pos, cur.setValue(RedstoneLampBlock.LIT, lit), 3);
                if (lit) {
                    for (int k = 0; k < 4; k++)
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                                f.pos.getX() + RAND.nextDouble() * 1.2 - 0.1, f.pos.getY() + RAND.nextDouble(),
                                f.pos.getZ() + RAND.nextDouble() * 1.2 - 0.1, 0, 0, 0);
                }
            }
            return false;
        });
    }

    static void clear() {
        ACTIVE.clear();
    }
}
