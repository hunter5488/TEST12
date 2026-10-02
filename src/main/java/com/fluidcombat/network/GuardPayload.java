package com.fluidcombat.network;

import com.fluidcombat.FluidCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: guard key state. */
public record GuardPayload(boolean guarding) implements CustomPacketPayload {
    public static final Type<GuardPayload> TYPE = new Type<>(FluidCombat.id("guard"));
    public static final StreamCodec<ByteBuf, GuardPayload> CODEC = ByteBufCodecs.BOOL.map(GuardPayload::new, GuardPayload::guarding);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
