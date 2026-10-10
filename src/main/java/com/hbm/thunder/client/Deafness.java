package com.hbm.thunder.client;

import com.hbm.thunder.mixin.SoundEngineAccessor;
import com.hbm.thunder.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;

/** Blast deafness: every sound loses its highs and some volume, then hearing slowly comes back. */
final class Deafness {

    private static float strength;
    private static int age, duration;
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
        if (s <= 0 && !applied) return;
        Efx.deafHF = 1F - 0.98F * s;
        Efx.deafGain = 1F - 0.75F * s;
        applied = s > 0; // one more refresh after it ends, to lift the filter
        Minecraft mc = Minecraft.getInstance();
        ((SoundEngineAccessor) ((SoundManagerAccessor) mc.getSoundManager()).ntmdt$soundEngine())
                .ntmdt$channelAccess().executeOnChannels(Efx::refresh);
    }
}
