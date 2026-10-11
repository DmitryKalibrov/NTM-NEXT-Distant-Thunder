package com.hbm.thunder.neoforge;

import com.hbm.thunder.RumblePayload;
import com.hbm.thunder.client.DistantThunderClient;
import com.hbm.thunder.client.SoundRules;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeClient {

    private NeoForgeClient() {}

    static void init(IEventBus modBus) {
        SoundRules.SOUND_PHYSICS = ModList.get().isLoaded("sound_physics_remastered");
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> DistantThunderClient.tick(Minecraft.getInstance()));
        modBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAboveAll(DistantThunderClient.FLASH_LAYER, (graphics, delta) -> DistantThunderClient.renderFlash(graphics)));
    }

    static void receive(RumblePayload payload) {
        DistantThunderClient.receive(payload);
    }
}
