package com.fluidcombat.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class FCConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    // ---- common (gameplay, read on both sides)
    public static final ModConfigSpec.BooleanValue PLAYERS;
    public static final ModConfigSpec.BooleanValue MOBS;
    public static final ModConfigSpec.DoubleValue PLAYER_DAMAGE;
    public static final ModConfigSpec.DoubleValue MOB_DAMAGE;
    public static final ModConfigSpec.DoubleValue MOB_ATTACK_SPEED;
    public static final ModConfigSpec.DoubleValue MOB_COMBO_CHANCE;
    public static final ModConfigSpec.DoubleValue MOB_HEAVY_CHANCE;
    public static final ModConfigSpec.DoubleValue MOB_DEFENSE_CHANCE;
    public static final ModConfigSpec.BooleanValue MOB_DASH_ATTACKS;
    public static final ModConfigSpec.BooleanValue STAMINA;
    public static final ModConfigSpec.DoubleValue MAX_STAMINA;
    public static final ModConfigSpec.DoubleValue STAMINA_REGEN;
    public static final ModConfigSpec.IntValue PARRY_WINDOW;
    public static final ModConfigSpec.IntValue DODGE_IFRAMES;
    public static final ModConfigSpec.DoubleValue DODGE_STAMINA;
    public static final ModConfigSpec.BooleanValue FRIENDLY_SWEEPS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ENTITIES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_COMBATANTS;

    // ---- client
    public static final ModConfigSpec.BooleanValue FIRST_PERSON;
    public static final ModConfigSpec.BooleanValue THIRD_PERSON;
    public static final ModConfigSpec.DoubleValue CAMERA_SHAKE;
    public static final ModConfigSpec.DoubleValue CAMERA_SWAY;
    public static final ModConfigSpec.BooleanValue STAMINA_HUD;
    public static final ModConfigSpec.BooleanValue RIGHT_CLICK_GUARD;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("general");
        PLAYERS = b.comment("Replace vanilla melee combat for players.").define("players", true);
        MOBS = b.comment("Give weapon-wielding mobs and humanoid NPCs the combat system.").define("mobs", true);
        FRIENDLY_SWEEPS = b.comment("If false, player sweeps only hit hostile mobs, players, things attacking you, or the entity under the crosshair.")
                .define("friendlySweeps", false);
        EXCLUDED_ENTITIES = b.comment("Entity ids that always keep vanilla combat (the fluidcombat:excluded entity tag also works).")
                .defineListAllowEmpty("excludedEntities", List.of(), () -> "", o -> o instanceof String);
        EXTRA_COMBATANTS = b.comment("Extra entity ids that use the system even when unarmed (the fluidcombat:combatants entity tag also works).")
                .defineListAllowEmpty("extraCombatants", List.of(), () -> "", o -> o instanceof String);
        b.pop();

        b.push("damage");
        PLAYER_DAMAGE = b.comment("Global damage multiplier for player attacks made through the combat system.")
                .defineInRange("playerDamageMultiplier", 1.0, 0.0, 10.0);
        MOB_DAMAGE = b.comment("Global damage multiplier for mob attacks made through the combat system.")
                .defineInRange("mobDamageMultiplier", 1.0, 0.0, 10.0);
        b.pop();

        b.push("mobs");
        MOB_ATTACK_SPEED = b.comment("Animation/attack speed of mobs relative to players (1.0 = same timing).")
                .defineInRange("attackSpeed", 0.85, 0.3, 3.0);
        MOB_COMBO_CHANCE = b.comment("Chance that a mob continues its combo after each hit.")
                .defineInRange("comboChance", 0.55, 0.0, 1.0);
        MOB_HEAVY_CHANCE = b.comment("Chance that a mob opens with a heavy attack.")
                .defineInRange("heavyChance", 0.12, 0.0, 1.0);
        MOB_DEFENSE_CHANCE = b.comment("Base chance that a mob guards or dodges when it sees an attack coming. Scaled by difficulty and the skilled/clumsy entity tags.")
                .defineInRange("defenseChance", 0.3, 0.0, 1.0);
        MOB_DASH_ATTACKS = b.comment("Allow mobs to close distance with lunging dash attacks.").define("dashAttacks", true);
        b.pop();

        b.push("stamina");
        STAMINA = b.comment("Enable stamina. Attacks, dodges and blocks consume it.").define("enabled", true);
        MAX_STAMINA = b.defineInRange("max", 100.0, 10.0, 1000.0);
        STAMINA_REGEN = b.comment("Stamina regenerated per tick.").defineInRange("regenPerTick", 1.2, 0.0, 100.0);
        DODGE_STAMINA = b.defineInRange("dodgeCost", 22.0, 0.0, 1000.0);
        b.pop();

        b.push("defense");
        PARRY_WINDOW = b.comment("Ticks after raising your guard during which a hit is parried.").defineInRange("parryWindow", 5, 0, 40);
        DODGE_IFRAMES = b.comment("Invulnerability ticks granted by a dodge.").defineInRange("dodgeInvulnerability", 7, 0, 40);
        b.pop();
        SPEC = b.build();

        ModConfigSpec.Builder c = new ModConfigSpec.Builder();
        FIRST_PERSON = c.comment("Animate the first-person hand.").define("firstPersonAnimations", true);
        THIRD_PERSON = c.comment("Animate entity models (players, mobs, NPCs).").define("modelAnimations", true);
        CAMERA_SHAKE = c.comment("Screen shake strength on impacts.").defineInRange("cameraShake", 1.0, 0.0, 3.0);
        CAMERA_SWAY = c.comment("Camera roll/pitch that follows your swings.").defineInRange("cameraSway", 1.0, 0.0, 3.0);
        STAMINA_HUD = c.comment("Show the stamina bar under the crosshair.").define("staminaHud", true);
        RIGHT_CLICK_GUARD = c.comment("Hold use (right click) with a melee weapon to guard, when the off hand has nothing to use.")
                .define("rightClickGuard", true);
        CLIENT_SPEC = c.build();
    }

    private FCConfig() {
    }
}
