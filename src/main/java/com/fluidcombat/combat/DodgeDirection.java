package com.fluidcombat.combat;

import com.fluidcombat.anim.Animations;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public enum DodgeDirection {
    BACK(Animations.DODGE_BACK),
    LEFT(Animations.DODGE_LEFT),
    RIGHT(Animations.DODGE_RIGHT),
    FORWARD(Animations.ROLL_FORWARD);

    public final String animation;

    DodgeDirection(String animation) {
        this.animation = animation;
    }

    public static DodgeDirection byId(int id) {
        DodgeDirection[] v = values();
        return id >= 0 && id < v.length ? v[id] : BACK;
    }

    /** Impulse for a dodge in this direction for an entity facing {@code yaw}. */
    public Vec3 impulse(float yaw) {
        float rad = yaw * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(rad), 0, Mth.cos(rad));
        Vec3 left = new Vec3(Mth.cos(rad), 0, Mth.sin(rad));
        return switch (this) {
            case BACK -> fwd.scale(-0.62).add(0, 0.22, 0);
            case LEFT -> left.scale(0.62).add(0, 0.22, 0);
            case RIGHT -> left.scale(-0.62).add(0, 0.22, 0);
            case FORWARD -> fwd.scale(0.95);
        };
    }
}
