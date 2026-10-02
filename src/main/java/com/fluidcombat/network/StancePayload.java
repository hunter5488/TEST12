package com.fluidcombat.network;

import com.fluidcombat.FluidCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: an entity raised or lowered its guard. */
public record StancePayload(int entity, boolean guarding) implements CustomPacketPayload {
    public static final Type<StancePayload> TYPE = new Type<>(FluidCombat.id("stance"));
    public static final StreamCodec<ByteBuf, StancePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StancePayload::entity,
            ByteBufCodecs.BOOL, StancePayload::guarding,
            StancePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
