package com.fluidcombat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fluidcombat.anim.RotationMath;
import java.util.Random;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class RotationMathTest {
    private static float error(Quaternionf q) {
        Vector3f e = RotationMath.eulerZYX(q, new Vector3f());
        Quaternionf rebuilt = new Quaternionf().rotationZYX(e.z, e.y, e.x);
        float worst = 0;
        for (Vector3f axis : new Vector3f[]{new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)}) {
            worst = Math.max(worst, q.transform(new Vector3f(axis)).distance(rebuilt.transform(new Vector3f(axis))));
        }
        return worst;
    }

    @Test
    void sidewaysArmSurvivesTheGimbalSingularity() {
        // pitch the arm forward, then sweep it 90 degrees to the side: JOML's decomposition loses this one
        Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(90)).rotateX((float) Math.toRadians(-90));
        assertTrue(error(q) < 1e-3f);
        q = new Quaternionf().rotateY((float) Math.toRadians(-90)).rotateX((float) Math.toRadians(-90));
        assertTrue(error(q) < 1e-3f);
    }

    @Test
    void randomRotationsRoundTrip() {
        Random r = new Random(42);
        for (int i = 0; i < 50_000; i++) {
            Quaternionf q = new Quaternionf().rotateY(r.nextFloat() * 7).rotateX(r.nextFloat() * 7).rotateY(r.nextFloat() * 7);
            float err = error(q);
            assertTrue(err < 2e-3f, "error " + err + " for " + q);
        }
    }
}
