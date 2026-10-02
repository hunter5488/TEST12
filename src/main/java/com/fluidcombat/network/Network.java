package com.fluidcombat.network;

import com.fluidcombat.client.ClientPayloads;
import com.fluidcombat.combat.AttackType;
import com.fluidcombat.combat.CombatManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private Network() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");

        r.playToServer(AttackPayload.TYPE, AttackPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                CombatManager.requestAttack(sp, AttackType.byId(p.attackType()), p.aimed());
            }
        });
        r.playToServer(GuardPayload.TYPE, GuardPayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) CombatManager.setWantGuard(sp, p.guarding());
        });
        r.playToServer(DodgePayload.TYPE, DodgePayload.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) CombatManager.dodge(sp, p.direction(), false);
        });

        // Client handlers are only ever invoked on the physical client; the lambdas defer class loading.
        r.playToClient(AnimPayload.TYPE, AnimPayload.CODEC, (p, ctx) -> ClientPayloads.anim(p));
        r.playToClient(StancePayload.TYPE, StancePayload.CODEC, (p, ctx) -> ClientPayloads.stance(p));
        r.playToClient(FeedbackPayload.TYPE, FeedbackPayload.CODEC, (p, ctx) -> ClientPayloads.feedback(p));
        r.playToClient(SelfStatePayload.TYPE, SelfStatePayload.CODEC, (p, ctx) -> ClientPayloads.selfState(p));
    }
}
