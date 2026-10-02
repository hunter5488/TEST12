package com.fluidcombat.anim;

import java.util.Arrays;

/** A full set of channel values for every {@link Bone}. */
public final class Pose {
    public final float[] v = new float[Bone.CHANNELS];

    public Pose() {
    }

    public Pose copyFrom(Pose other) {
        System.arraycopy(other.v, 0, v, 0, v.length);
        return this;
    }

    public Pose copy() {
        return new Pose().copyFrom(this);
    }

    public float get(Bone bone, int axis) {
        return v[bone.index(axis)];
    }

    public void set(Bone bone, float x, float y, float z) {
        int i = bone.ordinal() * 3;
        v[i] = x;
        v[i + 1] = y;
        v[i + 2] = z;
    }

    public void clear() {
        Arrays.fill(v, 0);
    }

    /** this = lerp(a, b, t). */
    public Pose lerp(Pose a, Pose b, float t) {
        for (int i = 0; i < v.length; i++) {
            v[i] = a.v[i] + (b.v[i] - a.v[i]) * t;
        }
        return this;
    }

    public boolean isZero() {
        for (float f : v) if (f != 0) return false;
        return true;
    }

    /** Mirrors a pose left/right (used for left-handed entities). */
    public Pose mirrored() {
        Pose out = new Pose();
        for (Bone b : Bone.VALUES) {
            Bone m = b.mirror();
            float x = get(b, 0), y = get(b, 1), z = get(b, 2);
            if (b == Bone.ROOT_POS || b == Bone.FP_POS) {
                out.set(m, -x, y, z);
            } else {
                out.set(m, x, -y, -z);
            }
        }
        return out;
    }
}
