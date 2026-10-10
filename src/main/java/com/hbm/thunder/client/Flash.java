package com.hbm.thunder.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

/** White screen flash, fading out fast in real time. */
final class Flash {

    private static float alpha;
    private static long last = System.nanoTime();

    private Flash() {}

    static void add(float a) {
        alpha = Math.max(alpha, Math.min(a, 0.95F));
    }

    static void render(GuiGraphicsExtractor g) {
        long now = System.nanoTime();
        alpha *= (float) Math.exp(-(now - last) / 1.0e9 * 3.5);
        last = now;
        if (alpha < 0.01F) return;
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), ARGB.colorFromFloat(alpha, 1F, 0.97F, 0.9F));
    }
}
