package com.fluidcombat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fluidcombat.anim.Animation;
import com.fluidcombat.anim.Animations;
import com.fluidcombat.anim.Bone;
import com.fluidcombat.anim.Easing;
import com.fluidcombat.anim.Pose;
import com.fluidcombat.combat.AttackMove;
import com.fluidcombat.combat.Moveset;
import com.fluidcombat.combat.Movesets;
import com.fluidcombat.combat.WeaponCategory;
import org.junit.jupiter.api.Test;

class AnimationTest {
    @Test
    void easingsHitTheirEndpoints() {
        for (Easing e : Easing.values()) {
            assertEquals(0f, e.apply(0f), 1e-4f, e.name());
            assertEquals(1f, e.apply(1f), 1e-4f, e.name());
        }
    }

    @Test
    void trackSpecIsParsedAndInterpolated() {
        Animation a = Animation.builder("t", 10)
                .track(Bone.RIGHT_ARM, "0:0 | 10:-90,40,20 L")
                .track(Bone.ROOT_POS, "0:0,1,0 | 5:0,3,0 L | 10:0")
                .build();
        Pose p = new Pose();
        a.sample(5, p);
        assertEquals(-45f, p.get(Bone.RIGHT_ARM, 0), 1e-4f);
        assertEquals(20f, p.get(Bone.RIGHT_ARM, 1), 1e-4f);
        assertEquals(10f, p.get(Bone.RIGHT_ARM, 2), 1e-4f);
        assertEquals(3f, p.get(Bone.ROOT_POS, 1), 1e-4f);
        a.sample(100, p);
        assertEquals(-90f, p.get(Bone.RIGHT_ARM, 0), 1e-4f);
        assertTrue(a.overrides().contains(Bone.RIGHT_ARM), "arms override by default");
        assertFalse(a.overrides().contains(Bone.ROOT_POS));
    }

    @Test
    void mirroringSwapsSidesAndFlipsLateralChannels() {
        Pose p = new Pose();
        p.set(Bone.RIGHT_ARM, -90, 30, 10);
        p.set(Bone.ROOT_POS, 2, 1, -1);
        Pose m = p.mirrored();
        assertEquals(-90f, m.get(Bone.LEFT_ARM, 0));
        assertEquals(-30f, m.get(Bone.LEFT_ARM, 1));
        assertEquals(-10f, m.get(Bone.LEFT_ARM, 2));
        assertEquals(0f, m.get(Bone.RIGHT_ARM, 0));
        assertEquals(-2f, m.get(Bone.ROOT_POS, 0));
        assertEquals(1f, m.get(Bone.ROOT_POS, 1));
    }

    @Test
    void everyMoveHasAMatchingAnimationThatCoversIt() {
        for (WeaponCategory c : WeaponCategory.values()) {
            Moveset set = Movesets.get(c);
            assertNotNull(set, c.name());
            java.util.List<AttackMove> moves = new java.util.ArrayList<>(set.combo());
            moves.add(set.heavy());
            moves.add(set.dash());
            for (AttackMove m : moves) {
                Animation a = Animations.get(m.id());
                assertNotNull(a, "animation for " + m.id());
                assertTrue(a.length() >= m.activeEnd(), m.id() + " animation ends before its hit");
                assertTrue(m.chainAt() >= m.activeEnd() && m.chainAt() <= m.length(), m.id() + " chain point");
                assertTrue(a.hasTrack(Bone.FP_ROT) && a.hasTrack(Bone.FP_POS), m.id() + " needs first-person tracks");
            }
            assertNotNull(Animations.guardFor(c), "guard stance for " + c);
        }
    }
}
