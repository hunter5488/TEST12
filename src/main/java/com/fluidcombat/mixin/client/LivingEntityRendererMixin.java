package com.fluidcombat.mixin.client;

import com.fluidcombat.client.PoseApplier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    protected M model;

    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String SETUP_ANIM = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V";

    /** Our animations replace the vanilla swing, so keep it from twisting the torso underneath them. */
    @Inject(method = RENDER, at = @At(value = "INVOKE", target = SETUP_ANIM))
    private void fluidcombat$beforeSetupAnim(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                                             int light, CallbackInfo ci) {
        if (PoseApplier.isAnimating(entity)) model.attackTime = 0;
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = SETUP_ANIM, shift = At.Shift.AFTER))
    private void fluidcombat$afterSetupAnim(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                                            int light, CallbackInfo ci) {
        PoseApplier.apply(entity, model, partialTick, poseStack);
    }
}
