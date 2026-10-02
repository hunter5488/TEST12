package com.fluidcombat.network;

import com.fluidcombat.FluidCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the player dodged. The client already applied the movement. */
public record DodgePayload(byte direction) implements CustomPacketPayload {
    public static final Type<DodgePayload> TYPE = new Type<>(FluidCombat.id("dodge"));
    public static final StreamCodec<ByteBuf, DodgePayload> CODEC = ByteBufCodecs.BYTE.map(DodgePayload::new, DodgePayload::direction);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
