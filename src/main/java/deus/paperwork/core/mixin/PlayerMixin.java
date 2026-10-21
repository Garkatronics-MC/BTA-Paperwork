package deus.paperwork.core.mixin;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.entity.motion.CarriedEntity;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.ICarriable;
import net.minecraft.core.world.IVehicle;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class)
public class PlayerMixin {


	@Shadow
	@Nullable
	protected ICarriable heldObject;

	@Inject(method = "hurt", at = @At("HEAD"), remap = false, cancellable = true)
	public void test(Entity attacker, int damage, DamageType type, CallbackInfoReturnable<Boolean> cir) {
		IVehicle vehicle = ((Entity) (Object) this).vehicle;
		if (vehicle != null) {
			if (type == DamageType.FALL) {
				cir.setReturnValue(false);
			}
		}
	}

	@Redirect(
		method = "readAdditionalSaveData",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/world/ICarriable;createAndLoadCarriable(Lnet/minecraft/core/entity/Entity;Lcom/mojang/nbt/tags/CompoundTag;)Lnet/minecraft/core/world/ICarriable;"
		), remap = false
	)
	private ICarriable redirectHeldObjectLoad(Entity holder, CompoundTag tag) {
		if (tag.containsKey("type")) {
			if (tag.getString("type").equals("block")) {
				return ICarriable.createAndLoadCarriable((Player) (Object) this, tag);
			} else if (tag.getString("type").equals("entity")) {
				return CarriedEntity.createAndLoad((Player) (Object) this, tag);
			}
		}
		return null;
	}
}
