package com.fluidcombat.client;

import com.fluidcombat.anim.Bone;
import com.fluidcombat.anim.RotationMath;
import com.fluidcombat.config.FCConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Applies animation controllers to entity models. Works with every model that extends {@link HumanoidModel}
 * (players, zombies, skeletons, piglins, and virtually every modded NPC using a player-shaped model) as well as
 * {@link IllagerModel} (vindicators, pillagers...).
 */
public final class PoseApplier {
    private static final ResolvedPose VANILLA = new ResolvedPose();
    private static final ResolvedPose OUT = new ResolvedPose();
    private static final Quaternionf TMP = new Quaternionf();
    private static final Vector3f EULER = new Vector3f();
    /** Hip height in model space (pixels / 16). */
    private static final float HIP = 12f / 16f;

    private PoseApplier() {
    }

    private record Parts(ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        ModelPart get(int i) {
            return switch (i) {
                case 0 -> head;
                case 1 -> body;
                case 2 -> rightArm;
                case 3 -> leftArm;
                case 4 -> rightLeg;
                default -> leftLeg;
            };
        }
    }

    @Nullable
    private static Parts parts(EntityModel<?> model) {
        if (model instanceof HumanoidModel<?> h) {
            return new Parts(h.head, h.body, h.rightArm, h.leftArm, h.rightLeg, h.leftLeg);
        }
        if (model instanceof IllagerModel<?> il) {
            ModelPart root = il.root();
            return new Parts(root.getChild("head"), root.getChild("body"), root.getChild("right_arm"), root.getChild("left_arm"),
                    root.getChild("right_leg"), root.getChild("left_leg"));
        }
        return null;
    }

    public static boolean isAnimating(LivingEntity entity) {
        if (!FCConfig.THIRD_PERSON.get()) return false;
        AnimController c = ClientAnims.get(entity.getId());
        return c != null && c.isActive();
    }

    /** Called right after {@code setupAnim}, before the model and its layers are rendered. */
    public static void apply(LivingEntity entity, EntityModel<?> model, float partialTick, PoseStack poseStack) {
        if (!FCConfig.THIRD_PERSON.get()) return;
        AnimController c = ClientAnims.get(entity.getId());
        if (c == null || !c.isActive()) return;
        Parts parts = parts(model);
        if (parts == null) return;

        // Read the vanilla pose. Arm rotations are stored relative to the torso's yaw.
        float vanillaBodyYaw = parts.body.yRot;
        for (int i = 0; i < ResolvedPose.MODEL_BONES; i++) {
            ModelPart p = parts.get(i);
            VANILLA.q[i].rotationZYX(p.zRot, p.yRot, p.xRot);
        }
        VANILLA.q[2].premul(TMP.rotationY(-vanillaBodyYaw));
        VANILLA.q[3].premul(TMP.rotationY(-vanillaBodyYaw));
        java.util.Arrays.fill(VANILLA.extra, 0);

        c.evaluate(c.model, VANILLA, partialTick, OUT);

        // Body first: its yaw carries the arms around.
        write(parts.body, OUT.q[1]);
        float bodyYaw = parts.body.yRot;
        write(parts.head, OUT.q[0]);
        write(parts.rightArm, TMP.rotationY(bodyYaw).mul(OUT.q[2]));
        write(parts.leftArm, TMP.rotationY(bodyYaw).mul(OUT.q[3]));
        float rightRadius = Math.abs(parts.rightArm.getInitialPose().x);
        float leftRadius = Math.abs(parts.leftArm.getInitialPose().x);
        parts.rightArm.x = -Mth.cos(bodyYaw) * rightRadius;
        parts.rightArm.z = Mth.sin(bodyYaw) * rightRadius;
        parts.leftArm.x = Mth.cos(bodyYaw) * leftRadius;
        parts.leftArm.z = -Mth.sin(bodyYaw) * leftRadius;

        boolean grounded = !model.riding && (entity.getPose() == Pose.STANDING || entity.getPose() == Pose.CROUCHING);
        if (grounded) {
            write(parts.rightLeg, OUT.q[4]);
            write(parts.leftLeg, OUT.q[5]);
        }

        if (model instanceof HumanoidModel<?> h) {
            h.hat.copyFrom(h.head);
            if (model instanceof PlayerModel<?> pm) {
                pm.leftPants.copyFrom(pm.leftLeg);
                pm.rightPants.copyFrom(pm.rightLeg);
                pm.leftSleeve.copyFrom(pm.leftArm);
                pm.rightSleeve.copyFrom(pm.rightArm);
                pm.jacket.copyFrom(pm.body);
            }
        } else if (model instanceof IllagerModel<?> il && c.overridesArms()) {
            il.root().getChild("arms").visible = false;
            parts.rightArm.visible = true;
            parts.leftArm.visible = true;
        }

        if (grounded) {
            float px = OUT.get(Bone.ROOT_POS, 0), py = OUT.get(Bone.ROOT_POS, 1), pz = OUT.get(Bone.ROOT_POS, 2);
            float rx = OUT.get(Bone.ROOT, 0), ry = OUT.get(Bone.ROOT, 1), rz = OUT.get(Bone.ROOT, 2);
            if (px != 0 || py != 0 || pz != 0) poseStack.translate(px / 16f, py / 16f, pz / 16f);
            if (rx != 0 || ry != 0 || rz != 0) {
                poseStack.translate(0, HIP, 0);
                poseStack.mulPose(TMP.rotationZYX(rz * Mth.DEG_TO_RAD, ry * Mth.DEG_TO_RAD, rx * Mth.DEG_TO_RAD));
                poseStack.translate(0, -HIP, 0);
            }
        }
    }

    private static void write(ModelPart part, Quaternionf q) {
        RotationMath.eulerZYX(q, EULER);
        part.xRot = EULER.x;
        part.yRot = EULER.y;
        part.zRot = EULER.z;
    }

}
