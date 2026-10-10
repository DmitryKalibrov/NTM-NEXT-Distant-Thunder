package com.hbm.thunder.client;

import com.hbm.thunder.mixin.SoundEngineAccessor;
import com.hbm.thunder.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Listener-side acoustics, Sound Physics style: rays from the head measure how enclosed the space
 * is and how big. Enclosure sets how much reverb you hear, size sets how long it rings, and the
 * walls' material (by block sound type, so modded blocks fall in too) how loud and bright it is.
 */
final class Room {

    private static final int RAYS = 48;
    private static final double MAX = 64;
    /** |dir.y| at most this is a wall ray; above is roof, below is floor (ignored). */
    private static final double SIDE = 0.35;
    private static final Vec3[] DIRS = new Vec3[RAYS];

    static {
        double golden = Math.PI * (3 - Math.sqrt(5)); // Fibonacci sphere: even coverage
        for (int i = 0; i < RAYS; i++) {
            double y = 1 - (i + 0.5) * 2 / RAYS, r = Math.sqrt(1 - y * y), a = golden * i;
            DIRS[i] = new Vec3(Math.cos(a) * r, y, Math.sin(a) * r);
        }
    }

    /** Loose, porous stuff that swallows sound. */
    private static final Set<SoundType> SOFT = Set.copyOf(List.of(
            SoundType.SAND, SoundType.SUSPICIOUS_SAND, SoundType.GRAVEL, SoundType.SUSPICIOUS_GRAVEL,
            SoundType.ROOTED_DIRT, SoundType.MUD, SoundType.SOUL_SAND, SoundType.SOUL_SOIL,
            SoundType.GRASS, SoundType.WET_GRASS, SoundType.NYLIUM, SoundType.MOSS, SoundType.MOSS_CARPET,
            SoundType.AZALEA_LEAVES, SoundType.CHERRY_LEAVES, SoundType.LEAF_LITTER,
            SoundType.WOOL, SoundType.SNOW, SoundType.POWDER_SNOW, SoundType.SPONGE, SoundType.WET_SPONGE));
    /** Wood: reflects some, damps the highs. */
    private static final Set<SoundType> WOOD = Set.copyOf(List.of(
            SoundType.WOOD, SoundType.NETHER_WOOD, SoundType.BAMBOO_WOOD, SoundType.CHERRY_WOOD,
            SoundType.LADDER, SoundType.SCAFFOLDING, SoundType.CHISELED_BOOKSHELF, SoundType.SHELF,
            SoundType.HANGING_SIGN, SoundType.NETHER_WOOD_HANGING_SIGN, SoundType.BAMBOO_WOOD_HANGING_SIGN,
            SoundType.CHERRY_WOOD_HANGING_SIGN));

    private static float decay = 0.5F, send, delay = 0.01F, bright = 0.6F;

    private Room() {}

    static void tick(Minecraft mc, ClientLevel level, long tick) {
        if (tick % 4 != 0) return;
        Vec3 eye = mc.gameRenderer.mainCamera().position();
        // The floor is under you everywhere, so it says nothing about the room: counting it made an
        // open field half "enclosed" and gave footsteps a slap off the ground. A room needs both a
        // roof and walls, so enclosure = roof coverage x wall coverage; floor rays are skipped.
        int up = 0, upHits = 0, side = 0, sideHits = 0;
        double sum = 0, reflect = 0, hf = 0;
        for (Vec3 d : DIRS) {
            boolean roof = d.y > SIDE;
            if (!roof && d.y < -SIDE) continue;
            if (roof) up++; else side++;
            BlockHitResult r = level.clip(new ClipContext(eye, eye.add(d.scale(MAX)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (r.getType() == HitResult.Type.BLOCK) {
                if (roof) upHits++; else sideHits++;
                sum += r.getLocation().distanceTo(eye);
                SoundType t = level.getBlockState(r.getBlockPos()).getSoundType();
                // stone/metal/glass and anything unknown: hard, just a bit under a perfect mirror
                reflect += SOFT.contains(t) ? 0.1 : WOOD.contains(t) ? 0.45 : 0.85;
                hf += SOFT.contains(t) ? 0.1 : WOOD.contains(t) ? 0.25 : 0.6;
            }
        }
        int hits = upHits + sideHits;
        float enclosure = (float) upHits / up * sideHits / side;
        float size = hits > 0 ? (float) (sum / hits) : (float) MAX;
        float mat = hits > 0 ? (float) (reflect / hits) : 0F;
        float tBright = hits > 0 ? (float) (hf / hits) : 0.6F;

        // Open field ~0, forest/village a hint, house a short slap, cave the full tail.
        float tSend = Math.clamp((enclosure - 0.3F) / 0.55F, 0F, 1F) * mat;
        float tDecay = Math.clamp(0.25F + size * 0.22F, 0.25F, 9F) * (0.35F + 0.65F * enclosure) * (0.3F + 0.7F * mat);
        float tDelay = Math.clamp(size * 2 / 343F, 0.004F, 0.3F);

        send += (tSend - send) * 0.35F;
        decay += (tDecay - decay) * 0.35F;
        delay += (tDelay - delay) * 0.35F;
        bright += (tBright - bright) * 0.35F;
        float d = decay, s = send, dl = delay, b = bright;
        ((SoundEngineAccessor) ((SoundManagerAccessor) mc.getSoundManager()).ntmdt$soundEngine())
                .ntmdt$channelAccess().executeOnChannels(ignored -> Efx.room(d, s, dl, b));
    }
}
