package com.fluidcombat.combat;

import com.fluidcombat.FCAttachments;
import com.fluidcombat.FluidCombat;
import com.fluidcombat.anim.Animations;
import com.fluidcombat.config.FCConfig;
import com.fluidcombat.network.AnimPayload;
import com.fluidcombat.network.FeedbackPayload;
import com.fluidcombat.network.SelfStatePayload;
import com.fluidcombat.network.StancePayload;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/** Server-side authority for the combat system. */
public final class CombatManager {
    private static final ResourceLocation GUARD_SLOW = FluidCombat.id("guard_slow");

    private CombatManager() {
    }

    // =====================================================================================================
    // State access

    public static CombatState state(LivingEntity e) {
        CombatState s = e.getData(FCAttachments.STATE);
        CombatLogic.ensureStamina(s);
        return s;
    }

    @Nullable
    public static CombatState existing(LivingEntity e) {
        return e.getExistingData(FCAttachments.STATE).orElse(null);
    }

    public static double reach(LivingEntity e) {
        if (e instanceof Player p) return p.entityInteractionRange();
        return e.getBbWidth() * 0.5 + 1.7;
    }

    // =====================================================================================================
    // Attacking

    /** Requests an attack. Works for players (via network), mobs (via AI) and API callers alike. */
    public static boolean requestAttack(LivingEntity e, AttackType type, int aimedEntity) {
        if (e.level().isClientSide || !e.isAlive()) return false;
        if (e instanceof Player p && p.isSpectator()) return false;
        WeaponCategory cat = Eligibility.categoryFor(e);
        if (cat == null) return false;
        CombatState s = state(e);
        s.aimedEntity = aimedEntity;
        AttackMove move = CombatLogic.request(s, Movesets.get(cat), type);
        if (move != null) {
            start(e, s, move, cat);
            return true;
        }
        return s.buffered != null;
    }

    private static void start(LivingEntity e, CombatState s, AttackMove move, WeaponCategory cat) {
        if (s.guarding) setGuarding(e, s, false);
        float speed = CombatLogic.attackSpeed(e, cat, move, s);
        CombatLogic.consumeStamina(s, move.stamina());
        CombatLogic.begin(s, move, cat, speed, CombatLogic.isMirrored(e));
        LivingEntity target = mobTarget(e, s);
        s.attackYaw = target != null && !(e instanceof Player) ? yawTo(e, target) : e.getYRot();
        byte flags = (byte) (AnimPayload.ATTACK | (s.mirrored ? AnimPayload.MIRROR : 0));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, new AnimPayload(e.getId(), move.id(), speed, flags));
    }

    @Nullable
    private static LivingEntity mobTarget(LivingEntity e, CombatState s) {
        if (s.mobTarget != null && s.mobTarget.isAlive()) return s.mobTarget;
        return e instanceof Mob m ? m.getTarget() : null;
    }

    private static float yawTo(Entity from, Entity to) {
        return CombatLogic.yawTo(to.getX() - from.getX(), to.getZ() - from.getZ());
    }

    /**
     * Called from {@code Mob.doHurtTarget}. Converts the vanilla instant hit into a combat-system attack.
     *
     * @return true if vanilla damage must be suppressed
     */
    public static boolean interceptMobAttack(Mob mob, Entity target) {
        if (mob.level().isClientSide || CombatContext.isDelivering(mob)) return false;
        if (!(target instanceof LivingEntity living)) return false;
        WeaponCategory cat = Eligibility.categoryFor(mob);
        if (cat == null) return false;
        CombatState s = state(mob);
        if (s.move != null || s.staggerTicks > 0 || s.dodgeTicks > 0) return true;
        s.mobTarget = living;
        s.wantGuard = false;
        s.mobGuardTimer = 0;
        AttackMove heavy = Movesets.get(cat).heavy();
        float heavyChance = FCConfig.MOB_HEAVY_CHANCE.get().floatValue() * Math.max(1f, Eligibility.skill(mob));
        boolean useHeavy = mob.getRandom().nextFloat() < heavyChance && (!FCConfig.STAMINA.get() || s.stamina >= heavy.stamina());
        return requestAttack(mob, useHeavy ? AttackType.HEAVY : AttackType.LIGHT, -1);
    }

    // =====================================================================================================
    // Ticking

    public static void tick(LivingEntity e) {
        if (!e.hasData(FCAttachments.STATE)) return;
        CombatState s = existing(e);
        if (s == null || !e.isAlive()) return;

        if (s.staggerTicks > 0) s.staggerTicks--;
        if (s.dodgeTicks > 0) s.dodgeTicks--;
        if (s.dodgeCooldown > 0) s.dodgeCooldown--;
        if (s.guardBreakCooldown > 0) s.guardBreakCooldown--;
        CombatLogic.regenStamina(s);
        updateGuard(e, s);
        if (e instanceof Mob mob) mobTick(mob, s);

        AttackMove m = s.move;
        int ev = CombatLogic.tick(s);
        if (m != null && s.move == m) {
            if ((ev & CombatLogic.EV_ACTIVE_START) != 0) onActiveStart(e, s, m);
            float from = Math.max(s.prevTime, m.windup());
            float to = Math.min(s.time, m.activeEnd());
            if (to > from) sweep(e, s, m, from, to);
            if (s.move == m) {
                if ((ev & CombatLogic.EV_CHAIN) != 0 && s.buffered != null) {
                    WeaponCategory cat = Eligibility.categoryFor(e);
                    if (cat == null) cat = s.category;
                    AttackType type = s.buffered;
                    if (cat != null) start(e, s, CombatLogic.select(s, Movesets.get(cat), type), cat);
                    else CombatLogic.finish(s);
                } else if ((ev & CombatLogic.EV_END) != 0) {
                    CombatLogic.finish(s);
                }
            }
        }

        if (e instanceof ServerPlayer sp) syncSelf(sp, s);
    }

    private static void onActiveStart(LivingEntity e, CombatState s, AttackMove m) {
        boolean heavy = m.stagger() >= 12 || m.guardBreak();
        SoundEvent sound = m.slash() ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_NODAMAGE;
        float pitch = (heavy ? 0.75f : 1.05f) + e.getRandom().nextFloat() * 0.2f;
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), sound, e.getSoundSource(), m.slash() ? 0.45f : 0.8f, pitch);

        if (!(e instanceof Player) && m.lunge() > 0) {
            WeaponCategory cat = s.category;
            boolean dash = cat != null && Movesets.get(cat).dash() == m;
            float rad = s.attackYaw * Mth.DEG_TO_RAD;
            double strength = m.lunge() * (dash ? 0.85 : 0.5);
            e.setDeltaMovement(e.getDeltaMovement().add(-Mth.sin(rad) * strength, dash ? 0.08 : 0, Mth.cos(rad) * strength));
            e.hurtMarked = true;
        }
    }

    private static void mobTick(Mob mob, CombatState s) {
        if (s.staggerTicks > 0) mob.getNavigation().stop();
        LivingEntity target = mobTarget(mob, s);

        if (s.counterReady && s.move == null && s.staggerTicks <= 0) {
            s.counterReady = false;
            if (target != null && mob.distanceTo(target) < reach(mob) + target.getBbWidth()) {
                s.wantGuard = false;
                s.mobGuardTimer = 0;
                s.mobTarget = target;
                requestAttack(mob, AttackType.LIGHT, -1);
            }
        }

        AttackMove m = s.move;
        if (m == null) return;
        CombatState.Phase phase = s.phase();
        if (phase == CombatState.Phase.WINDUP && target != null) {
            s.attackYaw = Mth.approachDegrees(s.attackYaw, yawTo(mob, target), 25f);
        }
        if (phase != CombatState.Phase.RECOVERY) {
            mob.setYRot(s.attackYaw);
            mob.setYBodyRot(s.attackYaw);
            Vec3 v = mob.getDeltaMovement();
            mob.setDeltaMovement(v.x * 0.6, v.y, v.z * 0.6);
        }
        // Decide once per light move whether to keep the combo going.
        if (s.buffered == null && s.comboDecidedSerial != s.moveSerial && s.time >= m.chainAt() - 2
                && s.comboIndex != 0 && s.category != null && Movesets.get(s.category).combo().contains(m)) {
            s.comboDecidedSerial = s.moveSerial;
            if (target != null && target.isAlive()
                    && mob.distanceTo(target) < reach(mob) * 1.3 + target.getBbWidth() * 0.5 + 0.5
                    && mob.getRandom().nextFloat() < FCConfig.MOB_COMBO_CHANCE.get()) {
                s.buffered = AttackType.LIGHT;
                s.bufferTicks = 20;
            }
        }
    }

    // =====================================================================================================
    // Hit detection

    private static void sweep(LivingEntity e, CombatState s, AttackMove m, float from, float to) {
        float f0 = (from - m.windup()) / m.active();
        float f1 = (to - m.windup()) / m.active();
        float a0 = Mth.lerp(f0, m.arcFrom(), m.arcTo());
        float a1 = Mth.lerp(f1, m.arcFrom(), m.arcTo());
        if (s.mirrored) {
            a0 = -a0;
            a1 = -a1;
        }
        float lo = Math.min(a0, a1), hi = Math.max(a0, a1);
        float yaw = e instanceof Player ? e.getYRot() : s.attackYaw;
        double reach = reach(e) * m.reach();

        AABB box = e.getBoundingBox().inflate(reach + 0.5, 1.0, reach + 0.5);
        List<LivingEntity> candidates = e.level().getEntitiesOfClass(LivingEntity.class, box, t -> canHit(e, s, t));
        candidates.sort(Comparator.comparingDouble(e::distanceToSqr));
        for (LivingEntity t : candidates) {
            if (s.move != m) return;
            double dx = t.getX() - e.getX(), dz = t.getZ() - e.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double halfWidth = t.getBbWidth() * 0.5;
            if (dist - halfWidth > reach) continue;
            AABB tb = t.getBoundingBox();
            if (tb.maxY < e.getY() - 0.8 || tb.minY > e.getY() + e.getBbHeight() + 0.8) continue;
            if (dist > 0.5) {
                float angle = Mth.wrapDegrees(CombatLogic.yawTo(dx, dz) - yaw);
                float pad = (float) (Mth.atan2(halfWidth + 0.25, dist) * Mth.RAD_TO_DEG) + 6f;
                if (!inArc(angle, lo - pad, hi + pad)) continue;
            }
            if (!e.hasLineOfSight(t)) continue;
            s.hitEntities.add(t.getId());
            deliverHit(e, s, m, t, yaw);
        }
    }

    private static boolean inArc(float angle, float lo, float hi) {
        for (int k = -1; k <= 1; k++) {
            float a = angle + 360f * k;
            if (a >= lo && a <= hi) return true;
        }
        return false;
    }

    private static boolean canHit(LivingEntity e, CombatState s, LivingEntity t) {
        if (t == e || !t.isAlive() || !t.isPickable() || t.isSpectator() || s.hitEntities.contains(t.getId())) return false;
        if (t == e.getVehicle() || e.isPassengerOfSameVehicle(t) || e.isAlliedTo(t)) return false;
        if (t instanceof ArmorStand stand && stand.isMarker()) return false;
        if (t.getId() == s.aimedEntity) return true;
        if (e instanceof Player p) {
            if (t instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return false;
            if (FCConfig.FRIENDLY_SWEEPS.get()) return true;
            return t instanceof Enemy || t instanceof Player || (t instanceof Mob m && m.getTarget() == p)
                    || t == p.getLastHurtMob() || t == p.getLastHurtByMob();
        }
        LivingEntity target = mobTarget(e, s);
        return t == target || (t instanceof Mob m && m.getTarget() == e) || t == e.getLastHurtByMob();
    }

    private static void deliverHit(LivingEntity e, CombatState s, AttackMove m, LivingEntity t, float yaw) {
        CombatContext ctx = CombatContext.push(e, t, m);
        try {
            t.invulnerableTime = 0;
            if (e instanceof Player p) {
                p.attackStrengthTicker = 100000;
                p.attack(t);
            } else if (e instanceof Mob mob) {
                mob.doHurtTarget(t);
            } else {
                t.hurt(e.damageSources().mobAttack(e), (float) e.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        } finally {
            ctx.pop();
        }
        if (ctx.parried) return;
        if (ctx.blocked) {
            s.hitstop = 3;
            feedback(e, t, FeedbackPayload.BLOCK);
            return;
        }
        if (!ctx.landed) return;

        boolean heavy = m.stagger() >= 12 || m.guardBreak();
        s.hitstop = heavy ? 4 : 2;
        float rad = yaw * Mth.DEG_TO_RAD;
        if (m.knockback() > 0) t.knockback(m.knockback(), Mth.sin(rad), -Mth.cos(rad));

        if (e.level() instanceof ServerLevel level) {
            double y = t.getY(0.55);
            if (m.slash()) level.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), y, t.getZ(), 1, 0, 0, 0, 0);
            if (heavy) {
                level.sendParticles(ParticleTypes.CRIT, t.getX(), y, t.getZ(), 14, 0.3, 0.4, 0.3, 0.45);
                level.playSound(null, t.getX(), t.getY(), t.getZ(),
                        s.category == WeaponCategory.HEAVY ? SoundEvents.MACE_SMASH_GROUND : SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                        e.getSoundSource(), 0.9f, 0.85f);
            }
            if (!(e instanceof Player)) {
                level.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, e.getSoundSource(), 0.7f,
                        0.9f + e.getRandom().nextFloat() * 0.2f);
            }
        }

        CombatState vs = state(t);
        boolean armored = vs.move != null && vs.move.hyperArmor() && vs.phase() == CombatState.Phase.WINDUP;
        if (m.stagger() > 0 && !armored) {
            stagger(t, vs, m.stagger(), m.stagger() >= 10 ? Animations.STAGGER : Animations.FLINCH);
        } else if (vs.move != null && vs.phase() == CombatState.Phase.WINDUP && !armored) {
            stagger(t, vs, 6, Animations.FLINCH);
        } else if (vs.move == null) {
            playAnim(t, Animations.FLINCH, true);
        }
        feedback(e, t, heavy ? FeedbackPayload.HEAVY_HIT : FeedbackPayload.HIT);
    }

    // =====================================================================================================
    // Defence

    /** Damage pipeline hook: scales combat-system hits and applies dodges, guards and parries. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource src = event.getSource();
        Entity attacker = src.getEntity();
        CombatContext ctx = CombatContext.current();
        boolean ours = ctx != null && ctx.attacker == attacker && ctx.target == victim;

        // Fallback for mobs whose own doHurtTarget override never reaches Mob.doHurtTarget.
        if (!ours && attacker instanceof Mob mob && src.getDirectEntity() == mob && src.is(DamageTypes.MOB_ATTACK)
                && mob.getTarget() == victim && mob.distanceTo(victim) < 5 && Eligibility.categoryFor(mob) != null) {
            event.setCanceled(true);
            interceptMobAttack(mob, victim);
            return;
        }

        if (ours) {
            double mul = attacker instanceof Player ? FCConfig.PLAYER_DAMAGE.get() : FCConfig.MOB_DAMAGE.get();
            event.setAmount((float) (event.getAmount() * ctx.move.damage() * mul));
        }

        CombatState vs = existing(victim);
        if (vs == null || attacker == null || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        if (vs.dodgeTicks > 0) {
            event.setCanceled(true);
            if (!vs.perfectDodgeRewarded) {
                vs.perfectDodgeRewarded = true;
                vs.stamina = Math.min(CombatLogic.maxStamina(), vs.stamina + 15);
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                        victim.getSoundSource(), 0.8f, 1.7f);
                feedback(attacker instanceof LivingEntity la ? la : victim, victim, FeedbackPayload.PERFECT_DODGE);
            }
            return;
        }

        if (vs.guarding && !src.is(DamageTypeTags.BYPASSES_SHIELD) && isFrontal(victim, src)) {
            boolean melee = src.getDirectEntity() == attacker;
            if (melee && vs.guardTicks <= FCConfig.PARRY_WINDOW.get()) {
                event.setCanceled(true);
                parry(victim, vs, attacker, ctx);
                return;
            }
            if (ours && ctx.move.guardBreak()) {
                event.setAmount(event.getAmount() * 0.6f);
                guardBreak(victim, vs);
                return;
            }
            WeaponCategory cat = WeaponCategory.of(victim.getMainHandItem());
            if (cat == null) cat = WeaponCategory.FIST;
            float amount = event.getAmount();
            float blocked = amount * cat.guardReduction;
            event.setAmount(amount - blocked);
            CombatLogic.consumeStamina(vs, blocked * 3 + 6);
            if (ours) ctx.blocked = true;
            if (FCConfig.STAMINA.get() && vs.stamina <= 0) {
                guardBreak(victim, vs);
            } else {
                playAnim(victim, Animations.GUARD_HIT, true);
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK,
                        victim.getSoundSource(), 0.9f, 0.9f + victim.getRandom().nextFloat() * 0.3f);
                if (victim.level() instanceof ServerLevel level) {
                    Vec3 p = victim.getEyePosition().add(victim.getViewVector(1).scale(0.6));
                    level.sendParticles(ParticleTypes.CRIT, p.x, p.y - 0.3, p.z, 6, 0.15, 0.15, 0.15, 0.2);
                }
                if (victim instanceof Mob && victim.getRandom().nextFloat() < 0.35f * Eligibility.skill(victim)) vs.counterReady = true;
            }
        }
    }

    private static boolean isFrontal(LivingEntity victim, DamageSource src) {
        Vec3 pos = src.getSourcePosition();
        if (pos == null) return false;
        Vec3 view = victim.getViewVector(1).multiply(1, 0, 1).normalize();
        Vec3 to = pos.subtract(victim.position()).multiply(1, 0, 1).normalize();
        return view.dot(to) > 0.17;
    }

    private static void parry(LivingEntity victim, CombatState vs, Entity attacker, @Nullable CombatContext ctx) {
        if (ctx != null) ctx.parried = true;
        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK, victim.getSoundSource(), 1f, 1.5f);
        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ANVIL_LAND, victim.getSoundSource(), 0.3f, 1.8f);
        if (victim.level() instanceof ServerLevel level) {
            Vec3 p = victim.getEyePosition().add(victim.getViewVector(1).scale(0.7));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y - 0.3, p.z, 14, 0.15, 0.15, 0.15, 0.6);
        }
        vs.stamina = Math.min(CombatLogic.maxStamina(), vs.stamina + 10);
        playAnim(victim, Animations.PARRY, true);
        if (attacker instanceof LivingEntity la) {
            stagger(la, state(la), 26, Animations.STAGGER);
            Vec3 push = la.position().subtract(victim.position()).multiply(1, 0, 1).normalize();
            la.knockback(0.5, -push.x, -push.z);
            feedback(victim, la, FeedbackPayload.PARRY);
        }
        if (victim instanceof Mob) vs.counterReady = true;
    }

    private static void guardBreak(LivingEntity victim, CombatState vs) {
        setGuarding(victim, vs, false);
        vs.guardBreakCooldown = 40;
        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BREAK, victim.getSoundSource(), 1f, 0.9f);
        stagger(victim, vs, 30, Animations.GUARD_BREAK);
        feedback(victim, victim, FeedbackPayload.GUARD_BREAK);
    }

    public static void stagger(LivingEntity e, CombatState s, int ticks, String animation) {
        s.cancelMove();
        if (s.guarding) setGuarding(e, s, false);
        s.staggerTicks = Math.max(s.staggerTicks, ticks);
        playAnim(e, animation, true);
        if (e instanceof Mob mob) mob.getNavigation().stop();
        if (e instanceof ServerPlayer sp) syncSelf(sp, s);
    }

    // =====================================================================================================
    // Guarding

    public static void setWantGuard(LivingEntity e, boolean want) {
        if (Eligibility.categoryFor(e) == null && want) return;
        state(e).wantGuard = want;
    }

    private static void updateGuard(LivingEntity e, CombatState s) {
        if (s.wantGuard && s.move != null && s.phase() == CombatState.Phase.RECOVERY && s.staggerTicks <= 0) {
            CombatLogic.finish(s);
        }
        boolean can = s.wantGuard && s.move == null && s.staggerTicks <= 0 && s.dodgeTicks <= 0 && s.guardBreakCooldown <= 0
                && Eligibility.categoryFor(e) != null;
        if (can != s.guarding) setGuarding(e, s, can);
        if (s.guarding) s.guardTicks++;
    }

    private static void setGuarding(LivingEntity e, CombatState s, boolean guarding) {
        s.guarding = guarding;
        s.guardTicks = 0;
        AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (guarding && !speed.hasModifier(GUARD_SLOW)) {
                speed.addTransientModifier(new AttributeModifier(GUARD_SLOW, -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!guarding) {
                speed.removeModifier(GUARD_SLOW);
            }
        }
        if (s.lastBroadcastGuard != guarding) {
            s.lastBroadcastGuard = guarding;
            StancePayload payload = new StancePayload(e.getId(), guarding);
            if (e instanceof Player) PacketDistributor.sendToPlayersTrackingEntity(e, payload);
            else PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, payload);
        }
    }

    // =====================================================================================================
    // Dodging

    public static boolean dodge(LivingEntity e, byte directionId, boolean applyMotion) {
        if (e.level().isClientSide || !e.isAlive()) return false;
        CombatState s = state(e);
        if (s.staggerTicks > 0 || s.dodgeCooldown > 0) return false;
        if (s.move != null && s.phase() == CombatState.Phase.ACTIVE) return false;
        float cost = FCConfig.DODGE_STAMINA.get().floatValue();
        if (FCConfig.STAMINA.get() && s.stamina < cost * 0.5f) return false;
        DodgeDirection dir = DodgeDirection.byId(directionId);
        s.cancelMove();
        if (s.guarding) setGuarding(e, s, false);
        s.dodgeTicks = FCConfig.DODGE_IFRAMES.get();
        s.dodgeCooldown = 14;
        s.perfectDodgeRewarded = false;
        CombatLogic.consumeStamina(s, cost);
        playAnim(e, dir.animation, !(e instanceof Player));
        if (applyMotion) {
            e.setDeltaMovement(dir.impulse(e.getYRot()));
            e.hurtMarked = true;
        }
        return true;
    }

    // =====================================================================================================
    // Networking helpers

    public static void playAnim(LivingEntity e, String animation, boolean includeSelf) {
        AnimPayload payload = new AnimPayload(e.getId(), animation, 1f, (byte) 0);
        if (includeSelf) PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, payload);
        else PacketDistributor.sendToPlayersTrackingEntity(e, payload);
    }

    private static void feedback(LivingEntity attacker, LivingEntity victim, byte kind) {
        FeedbackPayload payload = new FeedbackPayload(attacker.getId(), victim.getId(), kind);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(attacker, payload);
    }

    private static void syncSelf(ServerPlayer sp, CombatState s) {
        boolean staggered = s.staggerTicks > 0;
        if (Math.abs(s.stamina - s.lastSyncedStamina) >= 1f || staggered != (s.lastSyncedStagger > 0) || s.guarding != s.lastSyncedGuard
                || (s.stamina >= CombatLogic.maxStamina() && s.lastSyncedStamina < CombatLogic.maxStamina())) {
            s.lastSyncedStamina = s.stamina;
            s.lastSyncedStagger = s.staggerTicks;
            s.lastSyncedGuard = s.guarding;
            PacketDistributor.sendToPlayer(sp, new SelfStatePayload(s.stamina, s.staggerTicks, s.guarding));
        }
    }
}
