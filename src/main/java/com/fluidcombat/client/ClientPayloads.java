package com.fluidcombat.client;

import com.fluidcombat.anim.Animation;
import com.fluidcombat.anim.Animations;
import com.fluidcombat.combat.AttackMove;
import com.fluidcombat.combat.CombatLogic;
import com.fluidcombat.combat.Movesets;
import com.fluidcombat.combat.WeaponCategory;
import com.fluidcombat.network.AnimPayload;
import com.fluidcombat.network.FeedbackPayload;
import com.fluidcombat.network.SelfStatePayload;
import com.fluidcombat.network.StancePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Client-side handlers for server payloads. Only ever invoked on the physical client. */
public final class ClientPayloads {
    private ClientPayloads() {
    }

    public static void anim(AnimPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(p.entity()) instanceof LivingEntity e)) return;
        if (e == mc.player && p.has(AnimPayload.ATTACK)) {
            AttackMove move = Movesets.byId(p.animation());
            if (move != null) ClientCombat.onServerAttack(mc.player, move, p.speed(), p.has(AnimPayload.MIRROR));
            return;
        }
        Animation anim = Animations.get(p.animation());
        if (anim != null) ClientAnims.getOrCreate(e.getId()).play(anim, p.speed(), p.has(AnimPayload.MIRROR), 2.5f);
    }

    public static void stance(StancePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(p.entity()) instanceof LivingEntity e)) return;
        WeaponCategory cat = WeaponCategory.of(e.getMainHandItem());
        if (cat == null) cat = WeaponCategory.FIST;
        AnimController c = p.guarding() ? ClientAnims.getOrCreate(e.getId()) : ClientAnims.get(e.getId());
        if (c != null) c.setStance(p.guarding() ? Animations.guardFor(cat) : null, CombatLogic.isMirrored(e));
    }

    public static void feedback(FeedbackPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Entity attacker = mc.level.getEntity(p.attacker());
        Entity victim = mc.level.getEntity(p.victim());
        boolean meAttacker = attacker == mc.player, meVictim = victim == mc.player;
        switch (p.kind()) {
            case FeedbackPayload.HIT, FeedbackPayload.HEAVY_HIT, FeedbackPayload.BLOCK -> {
                boolean heavy = p.kind() == FeedbackPayload.HEAVY_HIT;
                int stop = p.kind() == FeedbackPayload.BLOCK ? 3 : heavy ? 4 : 2;
                if (attacker != null) {
                    AnimController c = ClientAnims.get(attacker.getId());
                    if (c != null) c.hitstop = stop;
                }
                if (meAttacker) {
                    ClientCombat.LOCAL.hitstop = stop;
                    ClientCombat.addShake(heavy ? 0.9f : p.kind() == FeedbackPayload.BLOCK ? 0.3f : 0.35f);
                }
                if (meVictim) ClientCombat.addShake(heavy ? 1.1f : 0.5f);
            }
            case FeedbackPayload.PARRY -> {
                if (meAttacker || meVictim) ClientCombat.addShake(0.8f);
            }
            case FeedbackPayload.GUARD_BREAK -> {
                if (meVictim) {
                    ClientCombat.addShake(1.2f);
                    ClientCombat.LOCAL.guardBreakCooldown = 40;
                }
            }
            case FeedbackPayload.PERFECT_DODGE -> {
                if (meVictim) ClientCombat.addShake(0.15f);
            }
            default -> {
            }
        }
    }

    public static void selfState(SelfStatePayload p) {
        ClientCombat.stamina = p.stamina();
        ClientCombat.serverGuarding = p.guarding();
        ClientCombat.onServerStagger(p.stagger());
    }
}
