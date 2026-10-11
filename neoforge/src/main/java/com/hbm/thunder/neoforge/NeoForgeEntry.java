package com.hbm.thunder.neoforge;

import com.hbm.thunder.DistantThunder;
import com.hbm.thunder.RumblePayload;
import com.hbm.thunder.WorldShock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(DistantThunder.ID)
public final class NeoForgeEntry {

    public NeoForgeEntry(IEventBus modBus, Dist dist) {
        DistantThunder.init(FMLPaths.CONFIGDIR.get());
        DistantThunder.sender = (player, payload) -> PacketDistributor.sendToPlayer(player, payload);
        // the handler only ever runs on the client
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1")
                .playToClient(RumblePayload.TYPE, RumblePayload.CODEC, (payload, context) -> NeoForgeClient.receive(payload)));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> WorldShock.tick());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> WorldShock.clear());
        if (dist.isClient()) NeoForgeClient.init(modBus);
    }
}
