package com.fluidcombat.anim;

/**
 * Animatable channels. Every bone has three float channels (x, y, z).
 * <ul>
 *     <li>Model bones store rotations in degrees. Head, body and legs use vanilla ZYX euler angles.</li>
 *     <li>Arms use an animator friendly parameterisation: {@code x = pitch} (raise forward, -90 = pointing
 *     forward), {@code y = yaw} (sweep around the shoulder, positive = towards the entity's right) and
 *     {@code z = twist} around the arm's own axis (rolls the held item). Arm yaw is relative to the torso.</li>
 *     <li>{@link #ROOT} rotates the whole model around the hips (pitch, yaw, roll in degrees).</li>
 *     <li>{@link #ROOT_POS} offsets the whole model (in model pixels, 1/16 block; +y is down, -z is forward).</li>
 *     <li>{@link #FP_ROT} / {@link #FP_POS} drive the first-person hand (degrees / blocks in camera space).</li>
 * </ul>
 */
public enum Bone {
    HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, ROOT, ROOT_POS, FP_ROT, FP_POS;

    public static final Bone[] VALUES = values();
    public static final int COUNT = VALUES.length;
    public static final int CHANNELS = COUNT * 3;

    public int index(int axis) {
        return ordinal() * 3 + axis;
    }

    /** Bone that this bone maps to when an animation is mirrored for left-handed entities. */
    public Bone mirror() {
        return switch (this) {
            case RIGHT_ARM -> LEFT_ARM;
            case LEFT_ARM -> RIGHT_ARM;
            case RIGHT_LEG -> LEFT_LEG;
            case LEFT_LEG -> RIGHT_LEG;
            default -> this;
        };
    }

    public boolean isModelPart() {
        return ordinal() <= LEFT_LEG.ordinal();
    }
}
