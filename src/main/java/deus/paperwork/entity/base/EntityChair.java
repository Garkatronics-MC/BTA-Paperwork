package deus.paperwork.entity.base;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.player.gamemode.Gamemodes;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EntityChair extends Entity {
	public EntityChair(@Nullable World world) {
		super(Objects.requireNonNull(world));
		this.setSize(0.8F, 0.8F);

	}

	@Override
	public boolean canInteract() {
		return true;
	}

	public boolean canRide() {
		return true;
	}
	public double getRideHeight() {
		return 0.4;
	}
	public void positionRider() {
		if (this.passenger != null) {
			double d = Math.cos((double)this.yRot * Math.PI / 180.0) * -0.05;
			double d1 = Math.sin((double)this.yRot * Math.PI / 180.0) * 0.1;
			this.passenger.setPos(this.x + d, this.y + this.getRideHeight() + this.passenger.getRidingHeight(), this.z + d1);
		}
	}

	public boolean interact(@NotNull Player player) {
		if (super.interact(player)) {
			return true;
		} else if (this.world.isClientSide || this.passenger != null && this.passenger != player) {
			return false;
		} else {
			player.startRiding(this);
			return true;
		}
	}
	public boolean isPickable() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		float friction = 0.41F;

		if (this.isInWater()) {
			friction = 0.8F;
		} else if (this.isInLava()) {
			friction = 0.5F;
		} else if (this.onGround) {
			int blockId = this.world.getBlockId(MathHelper.floor(this.x), MathHelper.floor(this.bb.minY) - 1, MathHelper.floor(this.z));
			if (blockId > 0) {
				friction = (Blocks.blocksList[blockId].friction * 1.51F);
			} else {
				friction = 0.546F;
			}
		}

		this.yd -= 0.4;

		this.xd *= friction;
		this.yd *= friction;
		this.zd *= friction;

		this.move(this.xd, this.yd, this.zd);
	}


	@Override
	public boolean hurt(Entity entity, int damage, DamageType type) {
		if (!this.world.isClientSide && !this.removed) {

			this.markHurt();

			if (entity instanceof Player && ((Player)entity).getGamemode() == Gamemodes.CREATIVE) {
				this.remove();
			} else {
				this.remove();
			}

			return true;
		}
		return true;
	}


	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public Entity ejectRider() {
		Entity entity = this.passenger;
		entity.fallDistance = 0;
		return super.ejectRider();
	}



	@Override
	protected void defineSynchedData() {

	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {

	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {

	}
}
