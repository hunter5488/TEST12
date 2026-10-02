package com.fluidcombat.network;

import com.fluidcombat.FluidCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: impact feedback (hit-stop, screen shake). */
public record FeedbackPayload(int attacker, int victim, byte kind) implements CustomPacketPayload {
    public static final byte HIT = 0;
    public static final byte HEAVY_HIT = 1;
    public static final byte BLOCK = 2;
    public static final byte PARRY = 3;
    public static final byte GUARD_BREAK = 4;
    public static final byte PERFECT_DODGE = 5;

    public static final Type<FeedbackPayload> TYPE = new Type<>(FluidCombat.id("feedback"));
    public static final StreamCodec<ByteBuf, FeedbackPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FeedbackPayload::attacker,
            ByteBufCodecs.VAR_INT, FeedbackPayload::victim,
            ByteBufCodecs.BYTE, FeedbackPayload::kind,
            FeedbackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
