package com.hbm.thunder.client;

import java.util.ArrayList;
import java.util.List;

/** Camera jolts in degrees, each decaying quadratically over its duration. Client thread only. */
public final class Shake {

    private static final class Jolt {
        final float amp;
        final int duration;
        int age;

        Jolt(float amp, int duration) {
            this.amp = amp;
            this.duration = duration;
        }
    }

    private static final List<Jolt> JOLTS = new ArrayList<>();

    private Shake() {}

    static void add(float amp, int duration) {
        JOLTS.add(new Jolt(amp, duration));
    }

    static void tick() {
        JOLTS.removeIf(j -> ++j.age >= j.duration);
    }

    /** Current amplitude in degrees. */
    public static float amplitude() {
        float sum = 0;
        for (Jolt j : JOLTS) {
            float left = 1F - (float) j.age / j.duration;
            sum += j.amp * left * left;
        }
        return Math.min(sum, 10F);
    }
}
