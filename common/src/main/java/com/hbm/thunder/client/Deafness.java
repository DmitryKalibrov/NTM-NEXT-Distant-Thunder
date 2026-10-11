package com.hbm.thunder.client;

import com.hbm.thunder.mixin.SoundEngineAccessor;
import com.hbm.thunder.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;

/**
 * Blast deafness: every sound loses its highs and some volume, then hearing slowly comes back.
 * Head under water muffles everything the same way (fades in and out over a few ticks).
 */
final class Deafness {

    private static float strength;
    private static int age, duration;
    private static float wet;
    private static boolean applied;

    private Deafness() {}

    /** strength 0..1. A stronger blast while deaf restarts the curve. */
    static void hit(float s, int ticks) {
        if (s <= current()) return;
        strength = s;
        duration = ticks;
        age = 0;
    }

    private static float current() {
        if (age >= duration) return 0;
        float left = 1F - (float) age / duration;
        return strength * left * left; // the last bit of hearing comes back slowly
    }

    static void tick() {
        float s = current();
        if (age < duration) age++;
        Minecraft mc = Minecraft.getInstance();
        float w = wet + ((underwater(mc) ? 1F : 0F) - wet) * 0.4F;
        wet = w < 0.01F ? 0F : w > 0.99F ? 1F : w;
        if (s <= 0 && wet <= 0 && !applied) return;
        float hf = (1F - 0.98F * s) * (1F - 0.85F * wet), gain = (1F - 0.75F * s) * (1F - 0.35F * wet);
        if (hf == Efx.deafHF && gain == Efx.deafGain) return; // steady under water: new sounds pick it up in onPlay
        Efx.deafHF = hf;
        Efx.deafGain = gain;
        applied = s > 0 || wet > 0; // one more refresh after it ends, to lift the filter
        ((SoundEngineAccessor) ((SoundManagerAccessor) mc.getSoundManager()).ntmdt$soundEngine())
                .ntmdt$channelAccess().executeOnChannels(Efx::refresh);
    }

    /** Listener's head in water (left to Sound Physics when it's installed). */
    static boolean underwater(Minecraft mc) {
        return SoundRules.acoustics() && mc.player != null && mc.player.isEyeInFluid(FluidTags.WATER);
    }
}
