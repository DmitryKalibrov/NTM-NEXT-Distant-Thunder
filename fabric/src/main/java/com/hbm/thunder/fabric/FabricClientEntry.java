package com.hbm.thunder.fabric;

import com.hbm.thunder.RumblePayload;
import com.hbm.thunder.client.DistantThunderClient;
import com.hbm.thunder.client.SoundRules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricClientEntry implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SoundRules.SOUND_PHYSICS = FabricLoader.getInstance().isModLoaded("sound_physics_remastered");
        ClientPlayNetworking.registerGlobalReceiver(RumblePayload.TYPE, (payload, context) -> DistantThunderClient.receive(payload));
        ClientTickEvents.END_CLIENT_TICK.register(DistantThunderClient::tick);
        HudElementRegistry.addLast(DistantThunderClient.FLASH_LAYER, (graphics, delta) -> DistantThunderClient.renderFlash(graphics));
    }
}
