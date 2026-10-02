package com.fluidcombat.mixin;

import com.fluidcombat.combat.CombatManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
    /** Converts instant vanilla melee hits into wind-up / swing / recovery attacks. */
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void fluidcombat$interceptAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (CombatManager.interceptMobAttack((Mob) (Object) this, target)) {
            cir.setReturnValue(false);
        }
    }
}
