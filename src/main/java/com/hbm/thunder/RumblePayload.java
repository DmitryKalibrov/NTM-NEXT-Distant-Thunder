package com.hbm.thunder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * "Something with this power went off here, and this is what it sounded like up close."
 * near: inside this radius the player already hears NTM's own sound. enclosed: it went off underground.
 */
public record RumblePayload(double x, double y, double z, byte kind, float power, Identifier sound,
                            float near, boolean enclosed) implements CustomPacketPayload {

    public static final Type<RumblePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(DistantThunder.ID, "rumble"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RumblePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, RumblePayload::x,
            ByteBufCodecs.DOUBLE, RumblePayload::y,
            ByteBufCodecs.DOUBLE, RumblePayload::z,
            ByteBufCodecs.BYTE, RumblePayload::kind,
            ByteBufCodecs.FLOAT, RumblePayload::power,
            Identifier.STREAM_CODEC, RumblePayload::sound,
            ByteBufCodecs.FLOAT, RumblePayload::near,
            ByteBufCodecs.BOOL, RumblePayload::enclosed,
            RumblePayload::new);

    @Override
    public Type<RumblePayload> type() {
        return TYPE;
    }
}
