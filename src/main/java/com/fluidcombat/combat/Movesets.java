package com.fluidcombat.combat;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/** Built-in movesets for every {@link WeaponCategory}. */
public final class Movesets {
    private static final Map<WeaponCategory, Moveset> BY_CATEGORY = new EnumMap<>(WeaponCategory.class);
    private static final Map<String, AttackMove> BY_ID = new HashMap<>();
    private static final Map<String, Integer> COMBO_INDEX = new HashMap<>();

    static {
        register(WeaponCategory.SWORD, new Moveset(List.of(
                AttackMove.builder("sword_slash_1").timing(4, 3, 8).chainDelay(3).damage(0.85f).arc(80, -80).lunge(0.22f).build(),
                AttackMove.builder("sword_slash_2").timing(3, 3, 8).chainDelay(3).damage(0.85f).arc(-80, 80).lunge(0.22f).build(),
                AttackMove.builder("sword_slash_3").timing(6, 3, 11).chainDelay(5).damage(1.3f).arc(18, -18).reach(1.05f)
                        .lunge(0.38f).stagger(8).stamina(16).build()),
                AttackMove.builder("sword_heavy").timing(9, 5, 12).damage(1.6f).arc(175, -185).reach(1.05f).lunge(0.15f)
                        .stamina(28).stagger(14).knockback(0.5f).guardBreak().hyperArmor().build(),
                AttackMove.builder("sword_dash").timing(3, 4, 10).damage(1.15f).arc(14, -14).reach(1.3f).lunge(0.85f)
                        .stamina(18).stagger(6).thrust().build()));

        register(WeaponCategory.AXE, new Moveset(List.of(
                AttackMove.builder("axe_chop_1").timing(7, 3, 9).chainDelay(4).damage(1.0f).arc(35, -25).lunge(0.3f)
                        .stamina(15).stagger(4).build(),
                AttackMove.builder("axe_sweep_2").timing(6, 4, 10).chainDelay(4).damage(1.05f).arc(-85, 85).lunge(0.25f)
                        .stamina(15).knockback(0.3f).build()),
                AttackMove.builder("axe_heavy").timing(13, 3, 14).damage(1.9f).arc(22, -22).reach(1.1f).lunge(0.35f)
                        .stamina(30).stagger(20).knockback(0.6f).guardBreak().hyperArmor().build(),
                AttackMove.builder("axe_dash").timing(5, 3, 12).damage(1.25f).arc(22, -22).reach(1.1f).lunge(0.8f)
                        .stamina(20).stagger(10).build()));

        register(WeaponCategory.HEAVY, new Moveset(List.of(
                AttackMove.builder("mace_smash_1").timing(8, 3, 11).chainDelay(5).damage(1.0f).arc(22, -22).lunge(0.3f)
                        .stamina(18).stagger(8).build(),
                AttackMove.builder("mace_swing_2").timing(8, 4, 12).chainDelay(5).damage(1.05f).arc(85, -85).lunge(0.25f)
                        .stamina(18).knockback(0.6f).build()),
                AttackMove.builder("mace_heavy").timing(14, 3, 16).damage(1.8f).arc(25, -25).reach(1.1f).lunge(0.3f)
                        .stamina(32).stagger(24).knockback(0.8f).guardBreak().hyperArmor().thrust().build(),
                AttackMove.builder("mace_dash").timing(5, 4, 12).damage(1.2f).arc(-70, 60).lunge(0.75f)
                        .stamina(22).stagger(8).knockback(0.6f).build()));

        register(WeaponCategory.SPEAR, new Moveset(List.of(
                AttackMove.builder("spear_thrust_1").timing(4, 3, 8).chainDelay(3).damage(0.9f).arc(10, -10).reach(1.25f)
                        .lunge(0.25f).thrust().build(),
                AttackMove.builder("spear_thrust_2").timing(3, 3, 8).chainDelay(3).damage(0.9f).arc(10, -10).reach(1.3f)
                        .lunge(0.25f).thrust().build(),
                AttackMove.builder("spear_sweep_3").timing(6, 4, 10).chainDelay(5).damage(1.1f).arc(90, -90).reach(1.15f)
                        .lunge(0.2f).knockback(0.5f).stamina(15).build()),
                AttackMove.builder("spear_heavy").timing(11, 4, 12).damage(1.7f).arc(12, -12).reach(1.5f).lunge(0.6f)
                        .stamina(28).stagger(14).guardBreak().hyperArmor().thrust().build(),
                AttackMove.builder("spear_dash").timing(3, 4, 10).damage(1.2f).arc(12, -12).reach(1.4f).lunge(1.0f)
                        .stamina(18).stagger(6).thrust().build()));

        register(WeaponCategory.FIST, new Moveset(List.of(
                AttackMove.builder("fist_jab").timing(2, 2, 5).chainDelay(1).damage(1.0f).arc(10, -10).reach(0.9f)
                        .lunge(0.15f).stamina(6).thrust().build(),
                AttackMove.builder("fist_cross").timing(2, 2, 6).chainDelay(2).damage(1.0f).arc(10, -10).reach(0.9f)
                        .lunge(0.18f).stamina(6).thrust().build(),
                AttackMove.builder("fist_hook").timing(3, 3, 6).chainDelay(3).damage(1.25f).arc(-55, 15).reach(0.9f)
                        .lunge(0.2f).stamina(8).thrust().build()),
                AttackMove.builder("fist_uppercut").timing(7, 3, 10).damage(1.8f).arc(10, -10).reach(0.9f).lunge(0.25f)
                        .stamina(18).stagger(10).knockback(0.5f).hyperArmor().thrust().build(),
                AttackMove.builder("fist_dash").timing(3, 3, 10).damage(1.4f).arc(12, -12).reach(1.0f).lunge(0.8f)
                        .stamina(14).stagger(6).thrust().build()));
    }

    private Movesets() {
    }

    public static void register(WeaponCategory category, Moveset moveset) {
        BY_CATEGORY.put(category, moveset);
        for (int i = 0; i < moveset.combo().size(); i++) {
            AttackMove m = moveset.combo().get(i);
            BY_ID.put(m.id(), m);
            COMBO_INDEX.put(m.id(), i);
        }
        BY_ID.put(moveset.heavy().id(), moveset.heavy());
        BY_ID.put(moveset.dash().id(), moveset.dash());
    }

    public static Moveset get(WeaponCategory category) {
        return BY_CATEGORY.get(category);
    }

    /** Position of a move inside its light combo, or -1 for heavy / dash moves. */
    public static int comboIndexOf(AttackMove move) {
        return COMBO_INDEX.getOrDefault(move.id(), -1);
    }

    @Nullable
    public static AttackMove byId(String id) {
        return BY_ID.get(id);
    }
}
