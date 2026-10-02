package com.fluidcombat.combat;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Marks a hit that is being delivered by the combat system, so damage events can scale it, suppress
 * vanilla sweeping, and report whether it landed, was blocked or parried.
 */
public final class CombatContext {
    private static final ThreadLocal<CombatContext> CURRENT = new ThreadLocal<>();

    public final LivingEntity attacker;
    public final LivingEntity target;
    public final AttackMove move;
    public boolean landed;
    public boolean blocked;
    public boolean parried;
    @Nullable private CombatContext previous;

    private CombatContext(LivingEntity attacker, LivingEntity target, AttackMove move) {
        this.attacker = attacker;
        this.target = target;
        this.move = move;
    }

    public static CombatContext push(LivingEntity attacker, LivingEntity target, AttackMove move) {
        CombatContext ctx = new CombatContext(attacker, target, move);
        ctx.previous = CURRENT.get();
        CURRENT.set(ctx);
        return ctx;
    }

    public void pop() {
        CURRENT.set(previous);
    }

    @Nullable
    public static CombatContext current() {
        return CURRENT.get();
    }

    public static boolean isDelivering(LivingEntity attacker) {
        CombatContext ctx = CURRENT.get();
        return ctx != null && ctx.attacker == attacker;
    }
}
