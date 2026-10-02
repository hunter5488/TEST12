package com.fluidcombat.combat;

import com.fluidcombat.FluidCombat;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

/**
 * Movesets are chosen by weapon category. Modded weapons are classified automatically by their item
 * abilities / class / attack damage, and packs can force a category through the item tags listed below.
 */
public enum WeaponCategory {
    SWORD(1.6f, 0.70f),
    AXE(0.9f, 0.60f),
    HEAVY(0.6f, 0.60f),
    SPEAR(1.1f, 0.60f),
    FIST(4.0f, 0.40f);

    public static final TagKey<Item> TAG_SWORD = tag("movesets/sword");
    public static final TagKey<Item> TAG_AXE = tag("movesets/axe");
    public static final TagKey<Item> TAG_HEAVY = tag("movesets/heavy");
    public static final TagKey<Item> TAG_SPEAR = tag("movesets/spear");
    /** Items that must keep vanilla combat. */
    public static final TagKey<Item> TAG_EXCLUDED = tag("excluded");

    /** Attack speed (attribute value) at which this category plays its animations at 1x speed. */
    public final float nominalAttackSpeed;
    /** Fraction of frontal damage negated while guarding with this category. */
    public final float guardReduction;

    WeaponCategory(float nominalAttackSpeed, float guardReduction) {
        this.nominalAttackSpeed = nominalAttackSpeed;
        this.guardReduction = guardReduction;
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, FluidCombat.id(path));
    }

    /**
     * @return the category for the stack, {@link #FIST} for an empty hand, or {@code null} when the item is not
     * a melee weapon and vanilla behaviour should be kept.
     */
    @Nullable
    public static WeaponCategory of(ItemStack stack) {
        if (stack.isEmpty()) return FIST;
        if (stack.is(TAG_EXCLUDED)) return null;
        if (stack.is(TAG_SWORD)) return SWORD;
        if (stack.is(TAG_AXE)) return AXE;
        if (stack.is(TAG_HEAVY)) return HEAVY;
        if (stack.is(TAG_SPEAR)) return SPEAR;

        Item item = stack.getItem();
        if (item instanceof MaceItem) return HEAVY;
        if (item instanceof TridentItem || stack.canPerformAction(ItemAbilities.TRIDENT_THROW)) return SPEAR;
        if (stack.is(ItemTags.SWORDS) || stack.canPerformAction(ItemAbilities.SWORD_SWEEP)) return SWORD;
        if (stack.is(ItemTags.AXES) || stack.canPerformAction(ItemAbilities.AXE_DIG)) return AXE;
        if (item instanceof DiggerItem) return AXE;
        if (item instanceof BlockItem) return null;
        if (hasAttackDamage(stack)) return SWORD;
        return null;
    }

    public static boolean hasAttackDamage(ItemStack stack) {
        ItemAttributeModifiers mods = stack.getAttributeModifiers();
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.attribute().value() == Attributes.ATTACK_DAMAGE.value() && e.modifier().amount() > 0) return true;
        }
        return false;
    }
}
