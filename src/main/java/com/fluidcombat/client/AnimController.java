package com.fluidcombat.client;

import com.fluidcombat.anim.Animation;
import com.fluidcombat.anim.Bone;
import com.fluidcombat.anim.Easing;
import com.fluidcombat.anim.Pose;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * Plays animations for one entity.
 * <p>
 * Two layers are evaluated on top of the vanilla pose: a looping <i>stance</i> (guard) and a one-shot
 * <i>action</i> (attack, dodge, reaction). Whenever either changes, the pose that was last shown is captured and
 * the controller cross-fades from it, in quaternion space, to the new target. That is what keeps chained combo
 * moves, interrupts and returns to idle fluid instead of snapping.
 */
public final class AnimController {
    @Nullable Animation action;
    float actionTime, prevActionTime, actionSpeed = 1;
    boolean actionMirror;

    @Nullable Animation stance;
    float stanceTime, prevStanceTime;
    boolean stanceMirror;

    /** Ticks the action clock stays frozen (impact hit-stop). */
    int hitstop;

    private float transDuration, transElapsed, prevTransElapsed;
    private boolean transitioning;
    int idleTicks;

    final Blender model = new Blender();
    final Blender firstPerson = new Blender();

    private final Pose scratch = new Pose();
    private final Quaternionf tmp = new Quaternionf();

    static final class Blender {
        final ResolvedPose last = new ResolvedPose();
        final ResolvedPose from = new ResolvedPose();
        boolean fromValid;
        int lastTick = -100;

        void capture() {
            fromValid = ClientAnims.ticks - lastTick <= 2;
            if (fromValid) from.copyFrom(last);
        }
    }

    public void play(Animation animation, float speed, boolean mirror, float blendTicks) {
        beginTransition(blendTicks);
        action = animation;
        actionTime = prevActionTime = 0;
        actionSpeed = speed;
        actionMirror = mirror;
        hitstop = 0;
    }

    public void stopAction(float blendTicks) {
        if (action == null) return;
        beginTransition(blendTicks);
        action = null;
    }

    public void setStance(@Nullable Animation animation, boolean mirror) {
        if (animation == stance && mirror == stanceMirror) return;
        beginTransition(animation == null ? 5 : 4);
        stance = animation;
        stanceMirror = mirror;
        stanceTime = prevStanceTime = 0;
    }

    @Nullable
    public Animation action() {
        return action;
    }

    private void beginTransition(float duration) {
        model.capture();
        firstPerson.capture();
        transDuration = Math.max(duration, 0.001f);
        transElapsed = prevTransElapsed = 0;
        transitioning = duration > 0;
    }

    public void tick() {
        prevActionTime = actionTime;
        if (action != null) {
            if (hitstop > 0) {
                hitstop--;
            } else {
                actionTime += actionSpeed;
                if (!action.loop() && actionTime >= action.length()) {
                    actionTime = prevActionTime = action.length();
                    stopAction(stance != null ? 4 : 5);
                }
            }
        }
        prevStanceTime = stanceTime;
        if (stance != null) stanceTime++;
        prevTransElapsed = transElapsed;
        if (transitioning) {
            transElapsed++;
            if (transElapsed >= transDuration + 1) transitioning = false;
        }
        idleTicks = isActive() ? 0 : idleTicks + 1;
    }

    public boolean isActive() {
        return action != null || stance != null || transitioning;
    }

    /** Whether the current action replaces the arms (used to hide crossed illager arms etc). */
    boolean overridesArms() {
        Animation a = action != null ? action : stance;
        return a != null && (a.hasTrack(Bone.RIGHT_ARM) && a.overrides().contains(Bone.RIGHT_ARM)
                || a.hasTrack(Bone.LEFT_ARM) && a.overrides().contains(Bone.LEFT_ARM));
    }

    /**
     * Evaluates the final pose into {@code out}.
     *
     * @param vanilla the pose vanilla produced this frame (identity for first person)
     */
    void evaluate(Blender blender, ResolvedPose vanilla, float partialTick, ResolvedPose out) {
        out.copyFrom(vanilla);
        if (stance != null) layer(out, stance, Mth.lerp(partialTick, prevStanceTime, stanceTime), stanceMirror);
        if (action != null) layer(out, action, Mth.lerp(partialTick, prevActionTime, actionTime), actionMirror);
        if (transitioning) {
            float t = Mth.clamp(Mth.lerp(partialTick, prevTransElapsed, transElapsed) / transDuration, 0, 1);
            float k = Easing.SMOOTH.apply(t);
            ResolvedPose from = blender.fromValid ? blender.from : vanilla;
            for (int i = 0; i < ResolvedPose.MODEL_BONES; i++) {
                out.q[i].set(tmp.set(from.q[i]).slerp(out.q[i], k));
            }
            for (int i = 0; i < ResolvedPose.EXTRA; i++) {
                boolean angle = i < 3 || (i >= 6 && i < 9);
                float a = from.extra[i], b = out.extra[i];
                out.extra[i] = angle ? a + Mth.wrapDegrees(b - a) * k : a + (b - a) * k;
            }
        }
        blender.last.copyFrom(out);
        blender.lastTick = ClientAnims.ticks;
    }

    private void layer(ResolvedPose target, Animation anim, float time, boolean mirror) {
        scratch.clear();
        anim.sample(time, scratch);
        Pose p = mirror ? scratch.mirrored() : scratch;
        for (Bone keyed : anim.keyedBones()) {
            Bone b = mirror ? keyed.mirror() : keyed;
            if (b.isModelPart()) {
                float x = p.get(b, 0) * Mth.DEG_TO_RAD, y = p.get(b, 1) * Mth.DEG_TO_RAD, z = p.get(b, 2) * Mth.DEG_TO_RAD;
                Quaternionf q = target.q[b.ordinal()];
                if (anim.overrides().contains(keyed)) {
                    if (b == Bone.RIGHT_ARM || b == Bone.LEFT_ARM) {
                        // pitch, yaw (around the shoulder), then twist around the arm itself
                        q.identity().rotateY(y).rotateX(x).rotateY(z);
                    } else {
                        q.rotationZYX(z, y, x);
                    }
                } else {
                    q.mul(tmp.rotationZYX(z, y, x));
                }
            } else {
                int i = ResolvedPose.extraIndex(b);
                target.extra[i] += p.get(b, 0);
                target.extra[i + 1] += p.get(b, 1);
                target.extra[i + 2] += p.get(b, 2);
            }
        }
    }
}
