package com.trainguy9512.locomotion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.animator.JointAnimatorDispatcher;
import com.trainguy9512.locomotion.animation.animator.entity.FirstPersonPlayerJointAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {


    @Shadow @Nullable public ClientLevel level;

    @Shadow private volatile boolean pause;

    @Shadow protected abstract boolean isLevelRunningNormally();

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 5))
    public void tickJointAnimators(CallbackInfo ci, @Local ProfilerFiller profilerFiller){
        profilerFiller.popPush("jointAnimatorTick");
        if (!this.pause && this.isLevelRunningNormally()) {
            // There's a condition in Minecraft.java that only allows this to run if the level != null, but the mixin does not know this.
            assert this.level != null;
            JointAnimatorDispatcher jointAnimatorDispatcher = JointAnimatorDispatcher.getInstance();
            jointAnimatorDispatcher.tickEntityJointAnimators(this.level.entitiesForRendering());
            jointAnimatorDispatcher.tickFirstPersonPlayerJointAnimator();
        }
    }

    @Inject(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    public void injectStartAttackHitBlock(CallbackInfoReturnable<Boolean> cir){
        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
            dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_MINING).setValue(true);
        });
    }

    @Inject(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"))
    public void injectStartAttackHitEntity(CallbackInfoReturnable<Boolean> cir){
        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
            dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_ATTACKING).setValue(true);
        });
    }

    @Inject(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"))
    public void injectStartAttackMiss(CallbackInfoReturnable<Boolean> cir){
        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
            dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_ATTACKING).setValue(true);
        });
    }
//
//    @Inject(method = "startUseItem", at = @At("HEAD"))
//    public void injectOnStartUseItem(CallbackInfo ci){
//        FirstPersonPlayerJointAnimator.INSTANCE.localAnimationDataContainer.setValue(FirstPersonPlayerJointAnimator.IS_USING_ITEM, true);
//    }

    /**
     * Sets the first person player's IS_MINING driver to be false if there is no attacked block.
     */
    @Inject(
            method = "continueAttack",
            at = @At("HEAD")
    )
    public void injectOnContinueAttackIsNotMining(boolean bl, CallbackInfo ci){
        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
            dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_MINING).setValue(false);
        });
    }

    /**
     * Sets the first person player's IS_MINING driver to be true if the attacked blcok is being broken.
     */
    @Inject(
            method = "continueAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V")
    )
    public void injectOnContinueAttackIsMining(boolean bl, CallbackInfo ci){
        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
            dataContainer.getDriver(FirstPersonPlayerJointAnimator.IS_MINING).setValue(true);
        });
    }

    /**
     * Play the block cracking particles only if the mining animation has entered its impact state.
     * Play the cracking particles as normal if the first person renderer config is disabled.
     */
//    TODO: If this is going to be re-implemented, NeoForge needs an alternative implementation.
//    @Redirect(
//            method = "continueAttack",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;crack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V"))
//    public void onlySpawnBreakParticlesOnPickaxeImpact(ParticleEngine instance, BlockPos pos, Direction side) {
//        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(dataContainer -> {
//            if (dataContainer.getDriverValue(FirstPersonPlayerJointAnimator.IS_MINING_IMPACTING) || !LocomotionMain.CONFIG.data().firstPersonPlayer.enableRenderer) {
//                for (float i = 0; i < 8; i++) {
//                    instance.crack(pos, side);
//                }
//            }
//        });
//    }
}
