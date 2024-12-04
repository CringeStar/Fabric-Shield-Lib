package com.github.crimsondawn45.fabricshieldlib.mixin;

import com.github.crimsondawn45.fabricshieldlib.lib.event.ShieldBlockCallback;
import com.github.crimsondawn45.fabricshieldlib.lib.object.FabricShieldUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin that allows custom shields to block damage.
 */
@SuppressWarnings("ALL")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/**
	 * @param attacker entity attacking the entity blocking.
	 */
	@Shadow
	protected abstract void takeShieldHit(LivingEntity attacker);

	@Inject(
			at = @At(value = "HEAD"),
			method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z"
		)
	private void invokeEvent(ServerWorld world, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callbackInfo) {
		// Handle shield blocking
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack activeItem = entity.getActiveItem();
        ShieldBlockCallback.EVENT.invoker().block(entity, source, amount, entity.getActiveHand(), activeItem);
	}

	@Inject(
			at = @At(value = "TAIL"),
			method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z"
		)
	private void damage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.isInvulnerableTo(world, source) || !entity.getWorld().isClient || !entity.isDead() || !(source.isIn(DamageTypeTags.IS_FIRE) && entity.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))) {
            if (amount > 0.0F && entity.blockedByShield(source)) {
                // Handle Shield
            	entity.damageShield(amount);
                if (!source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                    Entity sourceEntity = source.getSource();

                    if (sourceEntity instanceof LivingEntity) {
                        this.takeShieldHit((LivingEntity) sourceEntity);
                    }
                }
            }
        }
	}

	/**
	 * This mixin changes the return value of "itemStack.getItem() instanceof ShieldItem".
	 *
	 * @param item        "itemStack.getItem()", the item to be tested
	 * @param shieldClass always be "ShieldItem.class"
	 * @return If the item is a shield, return "Object.class" so that "instanceof"
	 *         always results in true. Otherwise, return "Integer.class". As Item is
	 *         not assignable from Integer, "instanceof" always results in false.
	 */
    /*
	@ModifyConstant(
			allow = 1,
			require = 1,
			method = "blockedByShield(Lnet/minecraft/entity/damage/DamageSource;)V",
			slice = @Slice(from = @At(value = "INVOKE",
				target = "Lnet/minecraft/entity/LivingEntity;getBlockingItem()Lnet/minecraft/item/ItemStack;")),
			constant = @Constant(classValue = ShieldItem.class)
		)
	private Class<?> instanceOfShieldItem(Object item, Class<?> shieldClass) {
		return FabricShieldUtils.isShieldItem((Item) item) ? Object.class : Integer.class;
	}
	*/

    /* I believe there is a MixinExtra's bug that prevents this Mixin from working:
     * https://github.com/LlamaLad7/MixinExtras/issues/98
     * However, I think this is a better Mixin than targeting `itemStack.getItem()`
     * Let's switch to this Mixin once MixinExtra fixes the bug.
	@ModifyExpressionValue(at = @At(value = "CONSTANT", args = "classValue=net.minecraft.item.ShieldItem"),
		method = "blockedByShield(Lnet/minecraft/entity/damage/DamageSource;)V")
	private boolean instanceOfShieldItem(boolean isVanillaShieldClass) {
		System.out.println("instanceOfShieldItem");
		return isVanillaShieldClass;
	}
	*/

    /**
     *
     * @param item
     * @return true if the item should be considered as a shield
     */
	@ModifyExpressionValue(
			allow = 1,
			require = 1,
			method = "blockedByShield(Lnet/minecraft/entity/damage/DamageSource;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;")
		)
	private Item instanceOfShieldItem(Item item) {
		return FabricShieldUtils.isShieldItem(item) ? Items.SHIELD : Items.AIR;
	}

	/*
	@WrapOperation(
			allow = 1,
			require = 1,
			method = "blockedByShield(Lnet/minecraft/entity/damage/DamageSource;)V",
			slice = @Slice(from = @At(value = "INVOKE",
				target = "Lnet/minecraft/entity/LivingEntity;getBlockingItem()Lnet/minecraft/item/ItemStack;")),
			constant = @Constant(classValue = ShieldItem.class)
		)
	private boolean instanceOfShieldItem(Object obj, Operation<Boolean> original) {
		return FabricShieldUtils.isShieldItem((Item) obj);
	}
	*/
}
