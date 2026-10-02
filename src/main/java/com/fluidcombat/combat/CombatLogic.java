package com.fluidcombat.combat;

import com.fluidcombat.config.FCConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Side-independent attack timeline rules. The server runs them authoritatively; the client runs the same rules
 * to predict its own attacks so input feels instant.
 */
public final class CombatLogic {
    /** Inputs made this many ticks before a move's chain point are buffered. */
    public static final int BUFFER_WINDOW = 7;
    /** Idle ticks after which the light combo restarts from the first move. */
    public static final int COMBO_RESET = 12;

    public static final int EV_ACTIVE_START = 1;
    public static final int EV_CHAIN = 2;
    public static final int EV_END = 4;

    private CombatLogic() {
    }

    /**
     * Handles an attack input.
     *
     * @return the move to start right now, or {@code null} if the input was rejected or buffered.
     */
    @Nullable
    public static AttackMove request(CombatState s, Moveset set, AttackType type) {
        if (s.staggerTicks > 0 || s.dodgeTicks > 0) return null;
        if (s.move != null) {
            if (s.time >= s.move.chainAt()) {
                return select(s, set, type);
            }
            if (s.time >= s.move.chainAt() - BUFFER_WINDOW) {
                s.buffered = type;
                s.bufferTicks = BUFFER_WINDOW + 2;
            }
            return null;
        }
        return select(s, set, type);
    }

    public static AttackMove select(CombatState s, Moveset set, AttackType type) {
        if (type == AttackType.LIGHT) {
            int index = s.move == null && s.idleTicks > COMBO_RESET ? 0 : s.comboIndex;
            index = Math.floorMod(index, set.combo().size());
            s.comboIndex = (index + 1) % set.combo().size();
            return set.light(index);
        }
        s.comboIndex = 0;
        return set.get(type, 0);
    }

    public static void begin(CombatState s, AttackMove move, WeaponCategory category, float speed, boolean mirrored) {
        s.move = move;
        s.category = category;
        s.time = 0;
        s.prevTime = 0;
        s.speed = speed;
        s.mirrored = mirrored;
        s.buffered = null;
        s.bufferTicks = 0;
        s.hitstop = 0;
        s.hitEntities.clear();
        s.moveSerial++;
        s.guarding = false;
        s.guardTicks = 0;
    }

    /** Advances the timeline one tick and reports what happened as a bit set of {@code EV_*} flags. */
    public static int tick(CombatState s) {
        if (s.move == null) {
            s.idleTicks++;
            s.prevTime = s.time;
            return 0;
        }
        if (s.hitstop > 0) {
            s.hitstop--;
            s.prevTime = s.time;
            return 0;
        }
        AttackMove m = s.move;
        s.prevTime = s.time;
        s.time += s.speed;
        int ev = 0;
        if (s.prevTime < m.windup() && s.time >= m.windup()) ev |= EV_ACTIVE_START;
        if (s.buffered != null) {
            if (s.time >= m.chainAt()) ev |= EV_CHAIN;
            else if (--s.bufferTicks <= 0) s.buffered = null;
        }
        if (s.time >= m.length()) ev |= EV_END;
        return ev;
    }

    public static void finish(CombatState s) {
        s.move = null;
        s.buffered = null;
        s.idleTicks = 0;
    }

    /** Playback speed derived from the attacker's attack speed relative to the category's nominal speed. */
    public static float attackSpeed(LivingEntity e, WeaponCategory category, AttackMove move, CombatState s) {
        float speed;
        if (e instanceof Player && e.getAttributes().hasAttribute(Attributes.ATTACK_SPEED)) {
            float attr = (float) e.getAttributeValue(Attributes.ATTACK_SPEED);
            speed = category == WeaponCategory.FIST ? 1.0f : Mth.clamp(attr / category.nominalAttackSpeed, 0.65f, 1.6f);
        } else {
            speed = FCConfig.MOB_ATTACK_SPEED.get().floatValue();
        }
        if (FCConfig.STAMINA.get() && s.stamina >= 0 && s.stamina < move.stamina()) speed *= 0.75f;
        return speed;
    }

    public static boolean isMirrored(LivingEntity e) {
        return e.getMainArm() == HumanoidArm.LEFT;
    }

    public static float maxStamina() {
        return FCConfig.MAX_STAMINA.get().floatValue();
    }

    public static void ensureStamina(CombatState s) {
        if (s.stamina < 0) s.stamina = maxStamina();
    }

    public static void consumeStamina(CombatState s, float amount) {
        if (!FCConfig.STAMINA.get()) return;
        ensureStamina(s);
        s.stamina = Math.max(0, s.stamina - amount);
        s.regenDelay = 16;
    }

    public static void regenStamina(CombatState s) {
        ensureStamina(s);
        if (s.regenDelay > 0) {
            s.regenDelay--;
            return;
        }
        float rate = FCConfig.STAMINA_REGEN.get().floatValue() * (s.guarding ? 0.35f : 1f);
        s.stamina = Math.min(maxStamina(), s.stamina + rate);
    }

    /** Yaw (degrees, Minecraft convention) from one point to another. */
    public static float yawTo(double dx, double dz) {
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
    }
}
