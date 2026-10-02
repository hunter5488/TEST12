package com.fluidcombat.network;

import com.fluidcombat.FluidCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: play an animation on an entity. */
public record AnimPayload(int entity, String animation, float speed, byte flags) implements CustomPacketPayload {
    public static final byte MIRROR = 1;
    /** The animation is an attack move; the owning client uses it to correct its prediction. */
    public static final byte ATTACK = 2;

    public static final Type<AnimPayload> TYPE = new Type<>(FluidCombat.id("anim"));
    public static final StreamCodec<ByteBuf, AnimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AnimPayload::entity,
            ByteBufCodecs.stringUtf8(64), AnimPayload::animation,
            ByteBufCodecs.FLOAT, AnimPayload::speed,
            ByteBufCodecs.BYTE, AnimPayload::flags,
            AnimPayload::new);

    public boolean has(byte flag) {
        return (flags & flag) != 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
