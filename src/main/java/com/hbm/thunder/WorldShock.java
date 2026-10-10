package com.hbm.thunder;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Server side of the shockwave touching the world: windows shatter and, underground, stalactites
 * shake loose, each when the front (speed of sound) reaches it. Only already-loaded chunks.
 */
final class WorldShock {

    private record Hit(ServerLevel level, BlockPos pos, long due, boolean glass) {}

    private static final List<Hit> QUEUE = new ArrayList<>();
    private static final RandomSource RAND = RandomSource.create();
    private static final int PER_TICK = 256;

    private WorldShock() {}

    static void blast(ServerLevel level, double x, double y, double z, DistantThunder.Kind kind, float l0, boolean enclosed) {
        // Radii from the level at which the effect happens: ~150 dB breaks windows, ~165 dB shakes ceilings loose.
        if (DistantThunder.breakGlass)
            scan(level, x, y, z, Acoustics.reach(l0, kind, Acoustics.GLASS) * DistantThunder.glassRadius, true);
        if (DistantThunder.caveIns && enclosed)
            scan(level, x, y, z, Acoustics.reach(l0, kind, Acoustics.CAVE_IN), false);
    }

    /**
     * Chunks to look at: only those around players (that is all the server keeps loaded anyway, and a
     * nuke's glass radius is kilometres wide - walking every chunk in it would stall the tick).
     */
    private static LongOpenHashSet chunksNearPlayers(ServerLevel level, double x, double z, double r) {
        LongOpenHashSet out = new LongOpenHashSet();
        int view = level.getServer().getPlayerList().getViewDistance();
        int minCx = SectionPos.blockToSectionCoord(Mth.floor(x - r)), maxCx = SectionPos.blockToSectionCoord(Mth.floor(x + r));
        int minCz = SectionPos.blockToSectionCoord(Mth.floor(z - r)), maxCz = SectionPos.blockToSectionCoord(Mth.floor(z + r));
        for (ServerPlayer p : level.players()) {
            int pcx = p.chunkPosition().x(), pcz = p.chunkPosition().z();
            for (int cx = Math.max(minCx, pcx - view); cx <= Math.min(maxCx, pcx + view); cx++)
                for (int cz = Math.max(minCz, pcz - view); cz <= Math.min(maxCz, pcz + view); cz++)
                    out.add(ChunkPos.pack(cx, cz));
        }
        return out;
    }

    private static boolean isGlass(BlockState s) {
        return (s.is(ConventionalBlockTags.GLASS_BLOCKS) || s.is(ConventionalBlockTags.GLASS_PANES))
                && !s.is(ConventionalBlockTags.GLASS_BLOCKS_TINTED); // tinted glass is blast glass
    }

    private static boolean isStalactite(BlockState s) {
        return s.is(Blocks.POINTED_DRIPSTONE) && s.getValue(BlockStateProperties.VERTICAL_DIRECTION) == Direction.DOWN;
    }

    private static void scan(ServerLevel level, double x, double y, double z, double r, boolean glass) {
        long now = level.getGameTime();
        double r2 = r * r;
        if (r < 1) return;
        int minSy = Math.max(level.getMinSectionY(), SectionPos.blockToSectionCoord(Mth.floor(y - r)));
        int maxSy = Math.min(level.getMaxSectionY(), SectionPos.blockToSectionCoord(Mth.floor(y + r)));
        for (long packed : chunksNearPlayers(level, x, z, r)) {
            int cx = ChunkPos.getX(packed), cz = ChunkPos.getZ(packed);
            {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (int sy = minSy; sy <= maxSy; sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    // Palette check: skips the ~4096-block loop for sections without a candidate.
                    if (section.hasOnlyAir() || !section.maybeHas(glass ? WorldShock::isGlass : WorldShock::isStalactite)) continue;
                    for (int i = 0; i < 4096; i++) {
                        int lx = i & 15, ly = (i >> 8) & 15, lz = (i >> 4) & 15;
                        BlockState s = section.getBlockState(lx, ly, lz);
                        if (glass ? !isGlass(s) : !isStalactite(s)) continue;
                        int bx = (cx << 4) + lx, by = (sy << 4) + ly, bz = (cz << 4) + lz;
                        double d2 = (bx - x) * (bx - x) + (by - y) * (by - y) + (bz - z) * (bz - z);
                        if (d2 > r2) continue;
                        double t = d2 / r2; // edge of the radius: only some panes go
                        if (RAND.nextDouble() > 1 - t * t) continue;
                        long due = now + Math.round(Math.sqrt(d2) / DistantThunder.soundSpeed * 20);
                        QUEUE.add(new Hit(level, new BlockPos(bx, by, bz), due, glass));
                    }
                }
            }
        }
    }

    static void tick() {
        int done = 0;
        for (int i = QUEUE.size() - 1; i >= 0 && done < PER_TICK; i--) {
            Hit hit = QUEUE.get(i);
            if (hit.level.getGameTime() < hit.due) continue;
            QUEUE.remove(i);
            done++;
            BlockState s = hit.level.getBlockState(hit.pos);
            if (hit.glass) {
                if (isGlass(s)) hit.level.destroyBlock(hit.pos, false);
            } else if (isStalactite(s)) {
                // Knock out the top piece; vanilla drops the rest of the stalactite.
                BlockPos root = hit.pos;
                while (isStalactite(hit.level.getBlockState(root.above()))) root = root.above();
                hit.level.destroyBlock(root, true);
            }
        }
    }

    static void clear() {
        QUEUE.clear();
    }
}
