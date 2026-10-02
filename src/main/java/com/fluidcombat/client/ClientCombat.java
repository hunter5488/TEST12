package com.fluidcombat.client;

import com.fluidcombat.anim.Animation;
import com.fluidcombat.anim.Animations;
import com.fluidcombat.combat.AttackMove;
import com.fluidcombat.combat.AttackType;
import com.fluidcombat.combat.CombatLogic;
import com.fluidcombat.combat.CombatState;
import com.fluidcombat.combat.DodgeDirection;
import com.fluidcombat.combat.Moveset;
import com.fluidcombat.combat.Movesets;
import com.fluidcombat.combat.WeaponCategory;
import com.fluidcombat.config.FCConfig;
import com.fluidcombat.network.AttackPayload;
import com.fluidcombat.network.DodgePayload;
import com.fluidcombat.network.GuardPayload;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/** The local player's side of the combat system: input, prediction and feel. */
public final class ClientCombat {
    /** Predicted copy of this player's server-side combat state. */
    static final CombatState LOCAL = new CombatState();
    /** Last authoritative stamina from the server (-1 = unknown). */
    static float stamina = -1;
    static boolean serverGuarding;
    static float shake, shakeO;

    private static boolean guardSent;
    private static boolean rightClickGuard;
    private static int playerId = -1;
    /** Moves started locally whose server echo has not arrived yet. */
    private static final Deque<String> PREDICTED = new ArrayDeque<>();

    private ClientCombat() {
    }

    static void reset() {
        LOCAL.cancelMove();
        LOCAL.staggerTicks = LOCAL.dodgeTicks = LOCAL.dodgeCooldown = LOCAL.guardBreakCooldown = 0;
        LOCAL.idleTicks = 100;
        stamina = -1;
        guardSent = false;
        rightClickGuard = false;
        serverGuarding = false;
        PREDICTED.clear();
        shake = shakeO = 0;
    }

    public static void addShake(float amount) {
        shake = Math.min(2.5f, shake + amount);
    }

    private static boolean enabled() {
        return FCConfig.PLAYERS.get();
    }

    // =====================================================================================================
    // Input

    static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.isSpectator() || !enabled()) return;

        if (event.isUseItem()) {
            if (FCConfig.RIGHT_CLICK_GUARD.get() && canRightClickGuard(p, mc.hitResult)) {
                event.setCanceled(true);
                event.setSwingHand(false);
                rightClickGuard = true;
            }
            return;
        }
        if (!event.isAttack()) return;

        WeaponCategory cat = WeaponCategory.of(p.getMainHandItem());
        if (cat == null || p.isUsingItem()) return;
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK && LOCAL.move == null) return; // let mining happen
        Entity aimed = hit instanceof EntityHitResult ehr ? ehr.getEntity() : null;
        if (aimed != null && !(aimed instanceof LivingEntity)) return; // item frames, boats, end crystals...

        event.setCanceled(true);
        event.setSwingHand(false);
        if (LOCAL.staggerTicks > 0) return;

        AttackType type = p.isShiftKeyDown() ? AttackType.HEAVY : p.isSprinting() ? AttackType.DASH : AttackType.LIGHT;
        PacketDistributor.sendToServer(new AttackPayload((byte) type.ordinal(), aimed != null ? aimed.getId() : -1));
        LOCAL.stamina = stamina;
        AttackMove move = CombatLogic.request(LOCAL, Movesets.get(cat), type);
        if (move != null) startLocal(p, move, cat);
    }

    private static boolean canRightClickGuard(LocalPlayer p, @Nullable HitResult hit) {
        ItemStack main = p.getMainHandItem();
        WeaponCategory cat = WeaponCategory.of(main);
        if (cat == null || cat == WeaponCategory.SPEAR || main.getUseAnimation() != UseAnim.NONE) return false;
        ItemStack off = p.getOffhandItem();
        if (!off.isEmpty() && !off.is(Items.TOTEM_OF_UNDYING)) return false;
        if (hit == null) return true;
        if (hit.getType() == HitResult.Type.BLOCK) return false;
        if (hit instanceof EntityHitResult ehr) return ehr.getEntity() instanceof Enemy || ehr.getEntity() instanceof Player;
        return true;
    }

    private static void startLocal(LocalPlayer p, AttackMove move, WeaponCategory cat) {
        LOCAL.stamina = stamina;
        float speed = CombatLogic.attackSpeed(p, cat, move, LOCAL);
        boolean mirror = CombatLogic.isMirrored(p);
        CombatLogic.begin(LOCAL, move, cat, speed, mirror);
        if (stamina >= 0 && FCConfig.STAMINA.get()) stamina = Math.max(0, stamina - move.stamina());
        PREDICTED.addLast(move.id());
        while (PREDICTED.size() > 4) PREDICTED.removeFirst();
        if (move != Movesets.get(cat).dash()) p.setSprinting(false);
        Animation anim = Animations.get(move.id());
        if (anim != null) ClientAnims.getOrCreate(p.getId()).play(anim, speed, mirror, 2.5f);
    }

    /** The server started an attack for us. Adopt it unless it is one we already predicted. */
    static void onServerAttack(LocalPlayer p, AttackMove move, float speed, boolean mirror) {
        if (PREDICTED.remove(move.id())) return;
        WeaponCategory cat = WeaponCategory.of(p.getMainHandItem());
        if (cat == null) cat = WeaponCategory.FIST;
        CombatLogic.begin(LOCAL, move, cat, speed, mirror);
        int index = Movesets.comboIndexOf(move);
        Moveset set = Movesets.get(cat);
        LOCAL.comboIndex = index < 0 ? 0 : (index + 1) % set.combo().size();
        Animation anim = Animations.get(move.id());
        if (anim != null) ClientAnims.getOrCreate(p.getId()).play(anim, speed, mirror, 2.5f);
    }

    static void onServerStagger(int ticks) {
        if (ticks > 0 && LOCAL.staggerTicks <= 0) {
            LOCAL.cancelMove();
            PREDICTED.clear();
        }
        LOCAL.staggerTicks = ticks;
    }

    // =====================================================================================================
    // Tick

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            if (playerId != -1) {
                reset();
                ClientAnims.clear();
                playerId = -1;
            }
            return;
        }
        if (mc.isPaused()) return;
        if (p.getId() != playerId) {
            reset();
            playerId = p.getId();
        }
        ClientAnims.tick(mc.level);
        shakeO = shake;
        shake *= 0.72f;
        if (shake < 0.01f) shake = 0;
        if (!enabled()) return;

        if (LOCAL.staggerTicks > 0) LOCAL.staggerTicks--;
        if (LOCAL.dodgeTicks > 0) LOCAL.dodgeTicks--;
        if (LOCAL.dodgeCooldown > 0) LOCAL.dodgeCooldown--;
        if (LOCAL.guardBreakCooldown > 0) LOCAL.guardBreakCooldown--;

        AttackMove m = LOCAL.move;
        int ev = CombatLogic.tick(LOCAL);
        if (m != null && LOCAL.move == m) {
            if ((ev & CombatLogic.EV_ACTIVE_START) != 0) lunge(p, m);
            if ((ev & CombatLogic.EV_CHAIN) != 0 && LOCAL.buffered != null) {
                WeaponCategory cat = WeaponCategory.of(p.getMainHandItem());
                if (cat == null) cat = LOCAL.category;
                if (cat != null) startLocal(p, CombatLogic.select(LOCAL, Movesets.get(cat), LOCAL.buffered), cat);
                else CombatLogic.finish(LOCAL);
            } else if ((ev & CombatLogic.EV_END) != 0) {
                CombatLogic.finish(LOCAL);
            }
        }
        if (LOCAL.move == null && LOCAL.idleTicks > 40) PREDICTED.clear();

        updateGuard(mc, p);
        while (Keybinds.DODGE.consumeClick()) tryDodge(mc, p);
    }

    private static void lunge(LocalPlayer p, AttackMove m) {
        if (m.lunge() <= 0 || !p.onGround()) return;
        WeaponCategory cat = LOCAL.category;
        boolean dash = cat != null && Movesets.get(cat).dash() == m;
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        double strength = m.lunge() * (dash ? 0.7 : 0.38);
        p.setDeltaMovement(p.getDeltaMovement().add(look.x * strength, dash ? 0.06 : 0, look.z * strength));
    }

    private static void updateGuard(Minecraft mc, LocalPlayer p) {
        WeaponCategory cat = WeaponCategory.of(p.getMainHandItem());
        if (!mc.options.keyUse.isDown()) rightClickGuard = false;
        boolean want = cat != null && mc.screen == null && (Keybinds.GUARD.isDown() || rightClickGuard);
        if (want != guardSent) {
            guardSent = want;
            PacketDistributor.sendToServer(new GuardPayload(want));
        }
        boolean visual = want && LOCAL.move == null && LOCAL.staggerTicks <= 0 && LOCAL.dodgeTicks <= 0 && LOCAL.guardBreakCooldown <= 0;
        if (want && LOCAL.move != null && LOCAL.phase() == CombatState.Phase.RECOVERY && LOCAL.staggerTicks <= 0) {
            CombatLogic.finish(LOCAL);
            AnimController c = ClientAnims.get(p.getId());
            if (c != null) c.stopAction(3);
            visual = true;
        }
        AnimController c = visual ? ClientAnims.getOrCreate(p.getId()) : ClientAnims.get(p.getId());
        if (c != null) c.setStance(visual ? Animations.guardFor(cat) : null, CombatLogic.isMirrored(p));
        if (visual) p.setSprinting(false);
    }

    private static void tryDodge(Minecraft mc, LocalPlayer p) {
        if (mc.screen != null || LOCAL.staggerTicks > 0 || LOCAL.dodgeCooldown > 0 || !p.onGround()) return;
        if (LOCAL.move != null && LOCAL.phase() == CombatState.Phase.ACTIVE) return;
        float cost = FCConfig.DODGE_STAMINA.get().floatValue();
        if (FCConfig.STAMINA.get() && stamina >= 0 && stamina < cost * 0.5f) return;

        boolean left = mc.options.keyLeft.isDown(), right = mc.options.keyRight.isDown();
        boolean up = mc.options.keyUp.isDown(), down = mc.options.keyDown.isDown();
        DodgeDirection dir = left && !right ? DodgeDirection.LEFT
                : right && !left ? DodgeDirection.RIGHT
                : up && !down ? DodgeDirection.FORWARD : DodgeDirection.BACK;

        Vec3 impulse = dir.impulse(p.getYRot());
        p.setDeltaMovement(impulse.x, Math.max(p.getDeltaMovement().y, impulse.y), impulse.z);
        p.setSprinting(false);
        LOCAL.cancelMove();
        PREDICTED.clear();
        LOCAL.dodgeTicks = FCConfig.DODGE_IFRAMES.get();
        LOCAL.dodgeCooldown = 14;
        if (stamina >= 0 && FCConfig.STAMINA.get()) stamina = Math.max(0, stamina - cost);
        Animation anim = Animations.get(dir.animation);
        if (anim != null) ClientAnims.getOrCreate(p.getId()).play(anim, 1f, false, 2f);
        PacketDistributor.sendToServer(new DodgePayload((byte) dir.ordinal()));
    }

    /** Commits the player to their swing: slower movement while attacking, staggered or guarding. */
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (!enabled()) return;
        float f = 1f;
        if (LOCAL.staggerTicks > 0) {
            f = 0.2f;
        } else if (LOCAL.move != null) {
            boolean dash = LOCAL.category != null && Movesets.get(LOCAL.category).dash() == LOCAL.move;
            f = switch (LOCAL.phase()) {
                case WINDUP, ACTIVE -> dash ? 1f : 0.35f;
                case RECOVERY -> 0.6f;
                default -> 1f;
            };
        }
        if (f < 1f) {
            event.getInput().forwardImpulse *= f;
            event.getInput().leftImpulse *= f;
        }
    }
}
