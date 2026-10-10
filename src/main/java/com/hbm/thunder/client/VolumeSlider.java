package com.hbm.thunder.client;

import com.hbm.thunder.DistantThunder;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** "Distant Thunder" slider in Music & Sounds; same value as "volume" in the config file. */
public final class VolumeSlider {

    private VolumeSlider() {}

    public static OptionInstance<Double> create() {
        return new OptionInstance<>(
                "options.ntm_distant_thunder.volume",
                OptionInstance.noTooltip(),
                (caption, v) -> v == 0
                        ? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF)
                        : Component.translatable("options.percent_value", caption, (int) (v * 100)),
                OptionInstance.UnitDouble.INSTANCE,
                (double) DistantThunder.volume,
                v -> {
                    DistantThunder.volume = v.floatValue();
                    DistantThunder.saveConfig();
                });
    }
}
