package com.fluidcombat.network;

import com.fluidcombat.FluidCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> owning client: authoritative stamina / stagger / guard state. */
public record SelfStatePayload(float stamina, int stagger, boolean guarding) implements CustomPacketPayload {
    public static final Type<SelfStatePayload> TYPE = new Type<>(FluidCombat.id("self_state"));
    public static final StreamCodec<ByteBuf, SelfStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SelfStatePayload::stamina,
            ByteBufCodecs.VAR_INT, SelfStatePayload::stagger,
            ByteBufCodecs.BOOL, SelfStatePayload::guarding,
            SelfStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
