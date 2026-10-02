package com.fluidcombat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fluidcombat.combat.AttackMove;
import com.fluidcombat.combat.AttackType;
import com.fluidcombat.combat.CombatLogic;
import com.fluidcombat.combat.CombatState;
import com.fluidcombat.combat.Moveset;
import com.fluidcombat.combat.Movesets;
import com.fluidcombat.combat.WeaponCategory;
import org.junit.jupiter.api.Test;

class CombatLogicTest {
    private final Moveset sword = Movesets.get(WeaponCategory.SWORD);

    private static int run(CombatState s, int ticks) {
        int all = 0;
        for (int i = 0; i < ticks; i++) all |= CombatLogic.tick(s);
        return all;
    }

    private AttackMove start(CombatState s, AttackType type) {
        AttackMove m = CombatLogic.request(s, sword, type);
        if (m != null) CombatLogic.begin(s, m, WeaponCategory.SWORD, 1f, false);
        return m;
    }

    @Test
    void firstLightStartsTheCombo() {
        CombatState s = new CombatState();
        assertSame(sword.light(0), start(s, AttackType.LIGHT));
        assertEquals(CombatState.Phase.WINDUP, s.phase());
    }

    @Test
    void activeStartFiresExactlyWhenWindupEnds() {
        CombatState s = new CombatState();
        AttackMove m = start(s, AttackType.LIGHT);
        assertEquals(0, run(s, m.windup() - 1) & CombatLogic.EV_ACTIVE_START);
        assertTrue((CombatLogic.tick(s) & CombatLogic.EV_ACTIVE_START) != 0);
        assertEquals(CombatState.Phase.ACTIVE, s.phase());
    }

    @Test
    void earlyInputIsBufferedAndChainsAtTheChainPoint() {
        CombatState s = new CombatState();
        AttackMove first = start(s, AttackType.LIGHT);
        run(s, first.chainAt() - 3);
        assertNull(CombatLogic.request(s, sword, AttackType.LIGHT), "too early to start, should buffer");
        assertEquals(AttackType.LIGHT, s.buffered);
        int ev = 0;
        while ((ev & CombatLogic.EV_CHAIN) == 0) ev = CombatLogic.tick(s);
        assertTrue(s.time >= first.chainAt());
        AttackMove next = CombatLogic.select(s, sword, s.buffered);
        assertSame(sword.light(1), next);
    }

    @Test
    void inputMuchTooEarlyIsDropped() {
        CombatState s = new CombatState();
        start(s, AttackType.LIGHT);
        assertNull(CombatLogic.request(s, sword, AttackType.LIGHT));
        assertNull(s.buffered);
    }

    @Test
    void comboWrapsAndResetsWhenIdle() {
        CombatState s = new CombatState();
        for (int i = 0; i < sword.combo().size(); i++) {
            AttackMove m = start(s, AttackType.LIGHT);
            assertSame(sword.light(i), m);
            run(s, m.length());
            CombatLogic.finish(s);
        }
        assertSame(sword.light(0), start(s, AttackType.LIGHT), "wraps after the finisher");
        run(s, s.move.length());
        CombatLogic.finish(s);
        start(s, AttackType.LIGHT);
        run(s, s.move.length());
        CombatLogic.finish(s);
        run(s, CombatLogic.COMBO_RESET + 1);
        assertSame(sword.light(0), start(s, AttackType.LIGHT), "resets after idling");
    }

    @Test
    void heavyAndDashResetTheCombo() {
        CombatState s = new CombatState();
        start(s, AttackType.LIGHT);
        CombatLogic.finish(s);
        assertSame(sword.heavy(), start(s, AttackType.HEAVY));
        CombatLogic.finish(s);
        assertSame(sword.light(0), start(s, AttackType.LIGHT));
    }

    @Test
    void staggeredOrDodgingEntitiesCannotAttack() {
        CombatState s = new CombatState();
        s.staggerTicks = 5;
        assertNull(CombatLogic.request(s, sword, AttackType.LIGHT));
        s.staggerTicks = 0;
        s.dodgeTicks = 3;
        assertNull(CombatLogic.request(s, sword, AttackType.LIGHT));
    }

    @Test
    void hitstopFreezesTheTimeline() {
        CombatState s = new CombatState();
        start(s, AttackType.LIGHT);
        run(s, 2);
        float t = s.time;
        s.hitstop = 3;
        run(s, 3);
        assertEquals(t, s.time);
        CombatLogic.tick(s);
        assertNotEquals(t, s.time);
    }

    @Test
    void yawMatchesMinecraftConvention() {
        assertEquals(0f, CombatLogic.yawTo(0, 1), 1e-3f, "south");
        assertEquals(90f, net.minecraft.util.Mth.wrapDegrees(CombatLogic.yawTo(-1, 0)), 1e-3f, "west");
        assertEquals(-90f, net.minecraft.util.Mth.wrapDegrees(CombatLogic.yawTo(1, 0)), 1e-3f, "east");
    }
}
