package com.hbm.thunder.fabric;

import com.hbm.thunder.DistantThunder;
import com.hbm.thunder.RumblePayload;
import com.hbm.thunder.WorldShock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricEntry implements ModInitializer {

    @Override
    public void onInitialize() {
        DistantThunder.init(FabricLoader.getInstance().getConfigDir());
        PayloadTypeRegistry.clientboundPlay().register(RumblePayload.TYPE, RumblePayload.CODEC);
        DistantThunder.sender = ServerPlayNetworking::send;
        ServerTickEvents.END_SERVER_TICK.register(server -> WorldShock.tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> WorldShock.clear());
    }
}
