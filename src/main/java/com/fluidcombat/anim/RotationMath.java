package com.fluidcombat.anim;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RotationMath {
    private RotationMath() {
    }

    /**
     * Decomposes a rotation into the ZYX euler angles that {@code ModelPart} uses ({@code rotationZYX(z, y, x)}).
     * <p>
     * JOML's own {@code getEulerAnglesZYX} collapses at the gimbal singularity (a yaw of +-90 degrees), which is
     * exactly where sideways swings live, so this works from the rotation matrix in double precision and handles
     * the singular case explicitly.
     */
    public static Vector3f eulerZYX(Quaternionf q, Vector3f out) {
        double x = q.x, y = q.y, z = q.z, w = q.w;
        double r00 = 1 - 2 * (y * y + z * z), r01 = 2 * (x * y - w * z), r02 = 2 * (x * z + w * y);
        double r10 = 2 * (x * y + w * z), r20 = 2 * (x * z - w * y), r21 = 2 * (y * z + w * x), r22 = 1 - 2 * (x * x + y * y);
        double cy = Math.sqrt(r21 * r21 + r22 * r22);
        if (cy < 2e-4) {
            out.y = (float) (r20 < 0 ? Math.PI / 2 : -Math.PI / 2);
            out.z = 0;
            out.x = (float) (r20 < 0 ? Math.atan2(r01, r02) : Math.atan2(-r01, -r02));
        } else {
            out.y = (float) Math.atan2(-r20, cy);
            out.x = (float) Math.atan2(r21, r22);
            out.z = (float) Math.atan2(r10, r00);
        }
        return out;
    }
}
