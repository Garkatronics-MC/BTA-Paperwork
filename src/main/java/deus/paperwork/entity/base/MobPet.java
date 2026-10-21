package deus.paperwork.entity.base;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.animal.MobSheep;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.*;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.helper.UUIDHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.primitives.AABBdc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MobPet extends MobAnimal {

	private Item tamingItem = Items.BONE;

	public MobPet(World world, Item tamingItem) {
		super(world);
		this.tamingItem = tamingItem;
	}

	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(16, (byte)0, Byte.class);
		this.entityData.define(17, (UUID) null, UUID.class);
		this.entityData.define(19, "", String.class);
	}

	public boolean isTamed() {
		return (this.entityData.getByte(16) & 4) != 0;
	}

	public void setTamed(boolean flag) {
		byte data = this.entityData.getByte(16);
		if (flag) {
			this.entityData.set(16, (byte)(data | 4));
		} else {
			this.entityData.set(16, (byte)(data & -5));
		}

	}

	void showHeartsOrSmokeFX(boolean doSmoke) {
		String s = "heart";
		if (!doSmoke) {
			s = "smoke";
		}

		for(int i = 0; i < 7; ++i) {
			double motionX = this.random.nextGaussian() * 0.02;
			double motionY = this.random.nextGaussian() * 0.02;
			double motionZ = this.random.nextGaussian() * 0.02;
			// this.world.spawnParticle(s, this.x + (double)(this.random.nextFloat() * this.bbWidth * 2.0F) - (double)this.bbWidth, this.y + 0.5 + (double)(this.random.nextFloat() * this.bbHeight), this.z + (double)(this.random.nextFloat() * this.bbWidth * 2.0F) - (double)this.bbWidth, motionX, motionY, motionZ, 0);
		}

	}

	public int getMaxHealth() {
		return this.isTamed() ? 20 : 8;
	}


	public @Nullable UUID getOwner() {
		return this.entityData.getUUID(17);
	}

	public void setOwner(UUID uuid) {
		this.entityData.set(17, uuid);
	}

	public boolean isSitting() {
		return (this.entityData.getByte(16) & 1) != 0;
	}

	public void setSitting(boolean flag) {
		byte data = this.entityData.getByte(16);
		if (flag) {
			this.entityData.set(16, (byte)(data | 1));
		} else {
			this.entityData.set(16, (byte)(data & -2));
		}

	}

	public boolean interact(@NotNull Player player) {
		if (super.interact(player)) {
			return true;
		} else {
			ItemStack itemstack = player.inventory.getCurrentItem();
			if (!this.isTamed()) {
				if (itemstack != null && itemstack.itemID == tamingItem.id) {
					itemstack.consumeItem(player);
					if (itemstack.stackSize <= 0) {
						player.inventory.setItem(player.inventory.getCurrentSlot(), (ItemStack) null);
					}

					if (!this.world.isClientSide) {
						if (this.random.nextInt(3) == 0) {
							this.setTamed(true);
							this.setPathToEntity((Path) null);
							this.setSitting(true);
							this.setHealthRaw(this.getMaxHealth());
							this.setOwner(player.uuid);
							this.showHeartsOrSmokeFX(true);
							this.world.sendTrackedEntityStatusUpdatePacket(this, (byte) 7);
						} else {
							this.showHeartsOrSmokeFX(false);
							this.world.sendTrackedEntityStatusUpdatePacket(this, (byte) 6);
						}
					}

					return true;
				}
			} else if(this.isTamed()) {
				setSitting(!isSitting());
			} else {
				if (itemstack != null && Item.itemsList[itemstack.itemID] instanceof ItemFood) {
					ItemFood itemfood = (ItemFood)Item.itemsList[itemstack.itemID];
					if (itemfood.getIsWolfsFavoriteMeat() && this.getHealth() < this.getMaxHealth()) {
						if (player.getGamemode().hasBlockConsumption()) {
							--itemstack.stackSize;
							if (itemstack.stackSize <= 0) {
								player.inventory.setItem(player.inventory.getCurrentSlot(), (ItemStack)null);
							}
						}

						this.heal(itemfood.getHealAmount(itemstack));
						return true;
					}
				}

			}

			return false;
		}
	}
	private void setPathToOwnerOrTeleport(Entity owner, float distance) {
		Path path = this.world.getPathToEntity(this, owner, 16.0F);
		if (path == null && distance > 12.0F) {
			int targetX = MathHelper.floor(owner.x);
			int targetY = MathHelper.floor(owner.bb.minY);
			int targetZ = MathHelper.floor(owner.z);
			boolean searchRadius = true;

			for (int _x = -2; _x <= 2; ++_x) {
				for (int _z = -2; _z <= 2; ++_z) {
					if ((Math.abs(_x) > 1 || Math.abs(_z) > 1) && this.world.isBlockNormalCube(targetX + _x, targetY - 1, targetZ + _z) && !this.world.isBlockNormalCube(targetX + _x, targetY, targetZ + _z) && !this.world.isBlockNormalCube(targetX + _x, targetY + 1, targetZ + _z)) {
						this.moveTo((double) ((float) (targetX + _x) + 0.5F), (double) targetY, (double) ((float) (targetZ + _z) + 0.5F), this.yRot, this.xRot);
						this.fallDistance = 0.0F;
						return;
					}
				}
			}
		} else {
			this.setPathToEntity(path);
		}
	}

	protected void updateAI() {
		super.updateAI();
		if (this.getTarget() instanceof EntityItem && (this.getTarget().isInWater() || this.getTarget().isInLava() || this.getTarget().isInWall() || !this.getTarget().onGround || this.getTarget().isRemoved())) {
			this.setTarget((Entity)null);
		}

		if (this.isSitting()) {
			this.setTarget((Entity)null);
		}

		if (!this.hasAttacked && !this.hasPath() && this.isTamed() && this.vehicle == null) {
			Player owner = this.world.getPlayerEntityByUUID(this.getOwner());
			if (owner != null) {
				float ownerDistance = owner.distanceTo(this);
				if (ownerDistance > 5.0F) {
					this.setPathToOwnerOrTeleport(owner, ownerDistance);
				}
			} else if (this.isInWater()) {
				this.setSitting(false);
			}
		} else if (this.getTarget() == null && !this.hasPath() && !this.isTamed() && this.world.rand.nextInt(100) == 0) {
			List<MobSheep> nearbySheep = this.world.getEntitiesWithinAABB(MobSheep.class, (AABBdc) AABB.fromPool(this.x, this.y, this.z, this.x + 1.0, this.y + 1.0, this.z + 1.0).grow(16.0, 4.0, 16.0));
			if (!nearbySheep.isEmpty()) {
				this.setTarget((Entity)nearbySheep.get(this.world.rand.nextInt(nearbySheep.size())));
			}
		}

		if (this.getTarget() == null) {
			ItemStack heldItemSlot = closestPlayer.getHeldItem();
			if (heldItemSlot == null || heldItemSlot.itemID <= 0) {
				List<EntityItem> triedItems = new ArrayList<>();

				org.joml.primitives.AABBf searchBox = new org.joml.primitives.AABBf(
					(float)this.x,
					(float)this.y,
					(float)this.z,
					(float)this.x + 1.0F,
					(float)this.y + 1.0F,
					(float)this.z + 1.0F
				);
				List<EntityItem> nearbyItems = this.world.getEntitiesWithinAABB(
					EntityItem.class,
					(AABBdc) searchBox
				);				if (!nearbyItems.isEmpty()) {
					while(triedItems.size() != nearbyItems.size()) {
						EntityItem item = (EntityItem)nearbyItems.get(this.world.rand.nextInt(nearbyItems.size()));
						if (!triedItems.contains(item)) {
							if (!item.isInWater() && !item.isInLava() && !item.isInWall() && item.onGround && !item.isRemoved()) {
								this.setTarget(item);
								break;
							}

							triedItems.add(item);
						}
					}
				}
			}
		}

		if (this.isInWater()) {
			this.setSitting(false);
		}

	}
	protected boolean canDespawn() {
		return !this.isTamed() && super.canDespawn();
	}

	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
//		super.addAdditionalSaveData(tag);
//		tag.putBoolean("Sitting", this.isSitting());
//		CompoundTag armorTag;
//		if (this.getHeldItem() != null) {
//			armorTag = new CompoundTag();
//			this.getHeldItem().writeToNBT(armorTag);
//			tag.putCompound("HeldItem", armorTag);
//		}
//
//
//		UUIDHelper.writeToTag(tag, this.getOwner(), "OwnerUUID");
	}

	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.setSitting(tag.getBoolean("Sitting"));


		String name;

		UUID ownerUUID = UUIDHelper.readFromTag(tag, "OwnerUUID");
		if (ownerUUID == null) {
			name = tag.getString("Owner");
			if (!name.isEmpty()) {
				UUIDHelper.runConversionAction(name, (uuid) -> {
					this.setOwner(uuid);
					this.setTamed(true);
				}, (UUIDHelper.StringFunction)null);
			}
		} else {
			this.setOwner(ownerUUID);
			this.setTamed(true);
		}

	}
}
