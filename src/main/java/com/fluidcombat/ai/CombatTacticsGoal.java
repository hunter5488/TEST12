package com.fluidcombat.ai;

import com.fluidcombat.combat.AttackType;
import com.fluidcombat.combat.CombatManager;
import com.fluidcombat.combat.CombatState;
import com.fluidcombat.combat.DodgeDirection;
import com.fluidcombat.combat.Eligibility;
import com.fluidcombat.config.FCConfig;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Flag-less goal that runs alongside a mob's normal melee AI and layers combat tactics on top:
 * reading the opponent's wind-ups to guard or dodge, and closing distance with dash attacks.
 */
public class CombatTacticsGoal extends Goal {
    private final PathfinderMob mob;

    public CombatTacticsGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return FCConfig.MOBS.get() && target != null && target.isAlive() && mob.distanceToSqr(target) < 100
                && Eligibility.categoryFor(mob) != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        CombatState s = CombatManager.existing(mob);
        if (s != null) {
            s.wantGuard = false;
            s.mobGuardTimer = 0;
        }
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        CombatState s = CombatManager.state(mob);
        double dist = mob.distanceTo(target);
        float skill = Eligibility.skill(mob) * difficulty();

        CombatState ts = CombatManager.existing(target);
        if (ts != null && ts.move != null && ts.phase() == CombatState.Phase.WINDUP && ts.moveSerial != s.reactedSerial) {
            s.reactedSerial = ts.moveSerial;
            double threat = CombatManager.reach(target) * ts.move.reach() + mob.getBbWidth() * 0.5 + 1.0;
            if (dist < threat && s.move == null && s.staggerTicks <= 0) {
                float chance = FCConfig.MOB_DEFENSE_CHANCE.get().floatValue() * skill;
                float roll = mob.getRandom().nextFloat();
                if (roll < chance * 0.6f && !ts.move.guardBreak()) {
                    s.wantGuard = true;
                    s.mobGuardTimer = 12 + mob.getRandom().nextInt(14);
                } else if (roll < chance) {
                    float r = mob.getRandom().nextFloat();
                    DodgeDirection dir = r < 0.5f ? DodgeDirection.BACK : r < 0.75f ? DodgeDirection.LEFT : DodgeDirection.RIGHT;
                    mob.setYRot(mob.getYHeadRot());
                    CombatManager.dodge(mob, (byte) dir.ordinal(), true);
                }
            }
        }

        if (s.mobGuardTimer > 0 && --s.mobGuardTimer == 0) s.wantGuard = false;

        if (!FCConfig.MOB_DASH_ATTACKS.get()) return;
        if (s.dashCooldown > 0) {
            s.dashCooldown--;
        } else if (s.move == null && !s.guarding && s.staggerTicks <= 0 && dist > 3.2 && dist < 6.0
                && mob.getRandom().nextFloat() < 0.04f * Math.min(skill, 1.5f) && mob.hasLineOfSight(target)) {
            s.dashCooldown = 60 + mob.getRandom().nextInt(80);
            s.mobTarget = target;
            CombatManager.requestAttack(mob, AttackType.DASH, -1);
        }
    }

    private float difficulty() {
        return switch (mob.level().getDifficulty()) {
            case PEACEFUL, EASY -> 0.5f;
            case NORMAL -> 1f;
            case HARD -> 1.4f;
        };
    }
}
