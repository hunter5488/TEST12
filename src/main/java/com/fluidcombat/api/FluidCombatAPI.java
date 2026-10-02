package com.fluidcombat.api;

import com.fluidcombat.anim.Animation;
import com.fluidcombat.anim.Animations;
import com.fluidcombat.combat.AttackType;
import com.fluidcombat.combat.CombatManager;
import com.fluidcombat.combat.CombatState;
import com.fluidcombat.combat.DodgeDirection;
import com.fluidcombat.combat.Moveset;
import com.fluidcombat.combat.Movesets;
import com.fluidcombat.combat.WeaponCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Public entry points for other mods.
 * <p>
 * Most NPC mods need nothing from here: any {@link Mob} that attacks through {@code Mob#doHurtTarget} while
 * holding a melee weapon (or whose type is in the {@code fluidcombat:combatants} entity tag) is driven by the
 * combat system automatically, and any entity rendered with a {@code HumanoidModel}/{@code PlayerModel} gets the
 * animations. Use these methods to drive NPCs from custom AI. All actions are server-side.
 */
public final class FluidCombatAPI {
    private FluidCombatAPI() {
    }

    /** Starts (or buffers) an attack for any eligible living entity. */
    public static boolean attack(LivingEntity entity, AttackType type) {
        return CombatManager.requestAttack(entity, type, -1);
    }

    /** Starts (or buffers) an attack aimed at a specific target. */
    public static boolean attack(Mob mob, LivingEntity target, AttackType type) {
        CombatManager.state(mob).mobTarget = target;
        return CombatManager.requestAttack(mob, type, target.getId());
    }

    public static void setGuarding(LivingEntity entity, boolean guarding) {
        CombatManager.setWantGuard(entity, guarding);
    }

    public static boolean dodge(LivingEntity entity, DodgeDirection direction) {
        return CombatManager.dodge(entity, (byte) direction.ordinal(), true);
    }

    public static void stagger(LivingEntity entity, int ticks) {
        CombatManager.stagger(entity, CombatManager.state(entity), ticks, ticks >= 10 ? Animations.STAGGER : Animations.FLINCH);
    }

    public static boolean isAttacking(LivingEntity entity) {
        CombatState s = CombatManager.existing(entity);
        return s != null && s.isAttacking();
    }

    public static boolean isGuarding(LivingEntity entity) {
        CombatState s = CombatManager.existing(entity);
        return s != null && s.guarding;
    }

    public static float getStamina(LivingEntity entity) {
        return CombatManager.state(entity).stamina;
    }

    /** Replaces the moveset of a weapon category. Register the matching animations on both sides. */
    public static void registerMoveset(WeaponCategory category, Moveset moveset) {
        Movesets.register(category, moveset);
    }

    public static void registerAnimation(Animation animation) {
        Animations.register(animation);
    }
}
