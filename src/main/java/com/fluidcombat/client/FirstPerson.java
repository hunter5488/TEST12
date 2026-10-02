package com.fluidcombat.client;

import com.fluidcombat.anim.Bone;
import com.fluidcombat.config.FCConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;

/** First-person hand animation and camera feel. */
public final class FirstPerson {
    private static final ResolvedPose IDENTITY = new ResolvedPose();
    private static final ResolvedPose OUT = new ResolvedPose();

    private FirstPerson() {
    }

    @Nullable
    private static ResolvedPose evaluate(LocalPlayer p, float partialTick) {
        AnimController c = ClientAnims.get(p.getId());
        if (c == null || !c.isActive()) return null;
        c.evaluate(c.firstPerson, IDENTITY, partialTick, OUT);
        return OUT;
    }

    static void onRenderHand(RenderHandEvent event) {
        if (!FCConfig.FIRST_PERSON.get() || event.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.isScoping()) return;
        ResolvedPose pose = evaluate(p, event.getPartialTick());
        if (pose == null || pose.extrasNearZero(Bone.FP_ROT) && pose.extrasNearZero(Bone.FP_POS)) return;

        float side = p.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        float pivotX = side * 0.56f, pivotY = -0.52f, pivotZ = -0.72f;
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(pose.get(Bone.FP_POS, 0), pose.get(Bone.FP_POS, 1), pose.get(Bone.FP_POS, 2));
        ps.translate(pivotX, pivotY, pivotZ);
        ps.mulPose(Axis.YP.rotationDegrees(pose.get(Bone.FP_ROT, 1)));
        ps.mulPose(Axis.XP.rotationDegrees(pose.get(Bone.FP_ROT, 0)));
        ps.mulPose(Axis.ZP.rotationDegrees(pose.get(Bone.FP_ROT, 2)));
        ps.translate(-pivotX, -pivotY, -pivotZ);
        mc.gameRenderer.itemInHandRenderer.renderArmWithItem(p, event.getPartialTick(), event.getInterpolatedPitch(),
                InteractionHand.MAIN_HAND, 0f, event.getItemStack(), event.getEquipProgress(), ps, event.getMultiBufferSource(),
                event.getPackedLight());
        ps.popPose();
        event.setCanceled(true);
    }

    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        float partial = (float) event.getPartialTick();

        float shake = Mth.lerp(partial, ClientCombat.shakeO, ClientCombat.shake) * FCConfig.CAMERA_SHAKE.get().floatValue();
        if (shake > 0.001f) {
            float t = p.tickCount + partial;
            event.setYaw(event.getYaw() + Mth.sin(t * 2.7f) * shake * 1.1f);
            event.setPitch(event.getPitch() + Mth.cos(t * 3.3f) * shake * 0.9f);
            event.setRoll(event.getRoll() + Mth.sin(t * 2.1f) * shake * 0.8f);
        }

        float sway = FCConfig.CAMERA_SWAY.get().floatValue();
        if (sway > 0 && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            ResolvedPose pose = evaluate(p, partial);
            if (pose != null) {
                event.setRoll(event.getRoll() - pose.get(Bone.FP_ROT, 2) * 0.05f * sway);
                event.setPitch(event.getPitch() - pose.get(Bone.FP_ROT, 0) * 0.025f * sway);
                event.setYaw(event.getYaw() - pose.get(Bone.FP_ROT, 1) * 0.02f * sway);
            }
        }
    }
}
