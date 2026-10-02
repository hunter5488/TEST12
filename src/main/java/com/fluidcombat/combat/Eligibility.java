package com.fluidcombat.combat;

import com.fluidcombat.FluidCombat;
import com.fluidcombat.config.FCConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Decides which entities take part in the combat system and with which moveset. */
public final class Eligibility {
    /** Entities that use the system even when unarmed (zombies, NPCs that punch...). */
    public static final TagKey<EntityType<?>> COMBATANTS = tag("combatants");
    /** Entities that never use the system. */
    public static final TagKey<EntityType<?>> EXCLUDED = tag("excluded");
    /** Entities that guard/dodge more often. */
    public static final TagKey<EntityType<?>> SKILLED = tag("skilled");
    /** Entities that rarely guard/dodge. */
    public static final TagKey<EntityType<?>> CLUMSY = tag("clumsy");

    private Eligibility() {
    }

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, FluidCombat.id(path));
    }

    private static boolean inList(EntityType<?> type, java.util.List<? extends String> list) {
        if (list.isEmpty()) return false;
        return list.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    public static boolean isExcluded(LivingEntity e) {
        return e.getType().is(EXCLUDED) || inList(e.getType(), FCConfig.EXCLUDED_ENTITIES.get()) || e instanceof ArmorStand;
    }

    /**
     * The moveset category this entity fights with right now, or {@code null} if it should use vanilla behaviour.
     * <p>
     * Mobs qualify when tagged as combatants, or when they hold a melee weapon and have a roughly humanoid body
     * (so foxes carrying swords in their mouths are left alone, but any modded NPC with a player-like body works).
     */
    @Nullable
    public static WeaponCategory categoryFor(LivingEntity e) {
        if (isExcluded(e)) return null;
        WeaponCategory held = WeaponCategory.of(e.getMainHandItem());
        if (e instanceof Player) {
            return FCConfig.PLAYERS.get() ? held : null;
        }
        if (!FCConfig.MOBS.get() || !(e instanceof Mob)) return null;
        boolean tagged = e.getType().is(COMBATANTS) || inList(e.getType(), FCConfig.EXTRA_COMBATANTS.get());
        // Tagged combatants punch when unarmed (held == FIST); holding a bow, crossbow or other non-weapon
        // (held == null) keeps their vanilla behaviour.
        if (tagged) return held;
        if (held != null && held != WeaponCategory.FIST && isHumanoidShaped(e)) return held;
        return null;
    }

    public static boolean isHumanoidShaped(LivingEntity e) {
        float w = e.getBbWidth(), h = e.getBbHeight();
        return h >= 1.2f && h <= 3.2f && w <= 1.2f && h > w * 1.6f;
    }

    /** Multiplier for mob defensive behaviour. */
    public static float skill(LivingEntity e) {
        if (e.getType().is(SKILLED)) return 1.6f;
        if (e.getType().is(CLUMSY)) return 0.35f;
        return 1f;
    }
}
