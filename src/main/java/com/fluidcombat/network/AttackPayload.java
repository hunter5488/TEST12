package com.fluidcombat.network;

import com.fluidcombat.FluidCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: attack input. {@code aimed} is the entity under the crosshair or -1. */
public record AttackPayload(byte attackType, int aimed) implements CustomPacketPayload {
    public static final Type<AttackPayload> TYPE = new Type<>(FluidCombat.id("attack"));
    public static final StreamCodec<ByteBuf, AttackPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, AttackPayload::attackType,
            ByteBufCodecs.VAR_INT, AttackPayload::aimed,
            AttackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
