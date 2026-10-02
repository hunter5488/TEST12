package com.fluidcombat.client;

import com.fluidcombat.anim.Bone;
import org.joml.Quaternionf;

/**
 * A pose in blendable form: model bones as quaternions (so blends never take the long way around or gimbal-lock)
 * and the root / first-person channels as plain floats.
 */
final class ResolvedPose {
    static final int MODEL_BONES = Bone.LEFT_LEG.ordinal() + 1;
    static final int EXTRA = (Bone.COUNT - MODEL_BONES) * 3;

    final Quaternionf[] q = new Quaternionf[MODEL_BONES];
    final float[] extra = new float[EXTRA];

    ResolvedPose() {
        for (int i = 0; i < MODEL_BONES; i++) q[i] = new Quaternionf();
    }

    static int extraIndex(Bone bone) {
        return (bone.ordinal() - MODEL_BONES) * 3;
    }

    float get(Bone bone, int axis) {
        return extra[extraIndex(bone) + axis];
    }

    ResolvedPose copyFrom(ResolvedPose other) {
        for (int i = 0; i < MODEL_BONES; i++) q[i].set(other.q[i]);
        System.arraycopy(other.extra, 0, extra, 0, EXTRA);
        return this;
    }

    void identity() {
        for (Quaternionf quat : q) quat.identity();
        java.util.Arrays.fill(extra, 0);
    }

    boolean extrasNearZero(Bone bone) {
        int i = extraIndex(bone);
        return Math.abs(extra[i]) < 1e-3f && Math.abs(extra[i + 1]) < 1e-3f && Math.abs(extra[i + 2]) < 1e-3f;
    }
}
