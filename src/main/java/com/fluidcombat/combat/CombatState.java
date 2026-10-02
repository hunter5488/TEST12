package com.fluidcombat.combat;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Per-entity combat state. Lives as a (transient) data attachment on the server and as the local player's
 * prediction state on the client.
 */
public class CombatState {
    public enum Phase { IDLE, WINDUP, ACTIVE, RECOVERY }

    // ---- attack timeline
    @Nullable public AttackMove move;
    @Nullable public WeaponCategory category;
    public float time;
    public float prevTime;
    public float speed = 1;
    public boolean mirrored;
    public int comboIndex;
    public int idleTicks = 100;
    public int hitstop;
    /** Incremented for every started move; lets observers react once per move. */
    public int moveSerial;
    @Nullable public AttackType buffered;
    public int bufferTicks;
    public final IntSet hitEntities = new IntOpenHashSet();
    public float attackYaw;
    public int aimedEntity = -1;

    // ---- defence
    public boolean wantGuard;
    public boolean guarding;
    public int guardTicks;
    public int guardBreakCooldown;
    public int staggerTicks;
    public int dodgeTicks;
    public int dodgeCooldown;
    public boolean perfectDodgeRewarded;

    // ---- stamina
    public float stamina = -1;
    public int regenDelay;

    // ---- mob AI
    @Nullable public LivingEntity mobTarget;
    public int reactedSerial = -1;
    public int comboDecidedSerial = -1;
    public int mobGuardTimer;
    public int dashCooldown = 40;
    public boolean counterReady;

    // ---- sync bookkeeping (server)
    public float lastSyncedStamina = -100;
    public int lastSyncedStagger = -1;
    public boolean lastSyncedGuard;
    public boolean lastBroadcastGuard;

    public Phase phase() {
        if (move == null) return Phase.IDLE;
        if (time < move.windup()) return Phase.WINDUP;
        if (time < move.activeEnd()) return Phase.ACTIVE;
        return Phase.RECOVERY;
    }

    public boolean isAttacking() {
        return move != null;
    }

    public void cancelMove() {
        move = null;
        buffered = null;
        bufferTicks = 0;
        hitstop = 0;
        idleTicks = 0;
    }
}
