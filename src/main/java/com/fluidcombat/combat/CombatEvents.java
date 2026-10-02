package com.fluidcombat.combat;

import com.fluidcombat.ai.CombatTacticsGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Game-bus listeners that connect the combat system to vanilla. */
public final class CombatEvents {
    private CombatEvents() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(CombatEvents::onEntityTick);
        bus.addListener(EventPriority.HIGH, CombatManager::onIncomingDamage);
        bus.addListener(CombatEvents::onDamagePost);
        bus.addListener(CombatEvents::onSweep);
        bus.addListener(CombatEvents::onJoin);
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide) {
            CombatManager.tick(living);
        }
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        CombatContext ctx = CombatContext.current();
        if (ctx != null && ctx.target == event.getEntity() && event.getSource().getEntity() == ctx.attacker
                && event.getNewDamage() > 0) {
            ctx.landed = true;
        }
    }

    /** Our swept arcs replace the vanilla sweep attack. */
    private static void onSweep(SweepAttackEvent event) {
        if (CombatContext.isDelivering(event.getEntity())) event.setSweeping(false);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof PathfinderMob mob && !Eligibility.isExcluded(mob)) {
            mob.goalSelector.addGoal(0, new CombatTacticsGoal(mob));
        }
    }
}
