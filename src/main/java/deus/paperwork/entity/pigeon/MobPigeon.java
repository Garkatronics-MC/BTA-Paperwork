package deus.paperwork.entity.pigeon;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.entity.paperplane.PaperPlaneRenderer;
import deus.paperwork.registry.PaperworkItems;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.core.WeightedRandomLootObject;

import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterEntityRenderer(renderer = MobPigeonRenderer.class)
@RegisterEntity(id = "pigeon", name = "pigeon")
public class MobPigeon extends MobAnimal {
	private static final Logger LOGGER = LogManager.getLogger(MobPigeon.class);
// 	protected @NotNull NamespaceID textureIdentifier = NamespaceID.getPermanent(MOD_ID, "pigeon");
	private static final int DATA_ITEM_STACK = 15;
	private static final int DATA_DELIVERING = 16;
	private static final int DATA_DEST_X = 17;
	private static final int DATA_DEST_Y = 18;
	private static final int DATA_DEST_Z = 19;

	private ItemStack itemStack = null;
	private int tox = 0;
	private int toy = 0;
	private int toz = 0;
	private boolean isDelivering = false;

	public float flap = 0.0F;
	public float flapSpeed = 0.0F;
	public float oFlapSpeed;
	public float oFlap;
	public float flapping = 1.0F;

	public MobPigeon(@Nullable World world) {
		super(world);
		this.setSize(0.6F, 0.5F); // Smaller size for a pigeon
		this.moveSpeed = 1.0F; // Slightly faster than walking
		this.mobDrops.add(new WeightedRandomLootObject(Items.FEATHER_CHICKEN.getDefaultStack(), 0, 2));
		this.textureIdentifier = NamespaceID.getPermanent(MOD_ID, "pigeon");


	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_ITEM_STACK, null, ItemStack.class);
		this.entityData.define(DATA_DELIVERING, (byte) 0, Byte.class);
		this.entityData.define(DATA_DEST_X, 0, Integer.class);
		this.entityData.define(DATA_DEST_Y, 0, Integer.class);
		this.entityData.define(DATA_DEST_Z, 0, Integer.class);
	}

	@Override
	public boolean canInteract() {
		return true;
	}

	@Override
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		if (this.isInWater() || this.isInLava()) {
			this.moveRelative(moveStrafing, moveForward, 0.02F);
			this.move(this.xd, this.yd, this.zd);
			this.xd *= 0.8;
			this.yd *= 0.8;
			this.zd *= 0.8;
			return;
		}

		// Simulate flight if delivering
		if (this.isDelivering && this.itemStack != null) {
			double dx, dy, dz;
			if (this.hasPath() && this.pathToEntity != null) {
				// Use pathfinding to guide horizontal movement, adjust Y manually
				dx = tox - this.x;
				dy = toy - this.y;
				dz = toz - this.z;
				// Normalize horizontal movement to follow path
				double distXZ = MathHelper.sqrt(dx * dx + dz * dz);
				if (distXZ > 0.1) {
					dx /= distXZ;
					dz /= distXZ;
					this.xd += dx * 0.1; // Horizontal speed
					this.zd += dz * 0.1;
				}
				// Adjust vertical movement to reach target Y
				if (Math.abs(dy) > 0.1) {
					this.yd += (dy > 0 ? 0.12 : -0.12); // Ascend or descend
				}
			} else {
				// Direct flight to destination if no path
				dx = tox - this.x;
				dy = toy - this.y;
				dz = toz - this.z;
				double dist = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
				if (dist > 0.1) {
					dx /= dist;
					dy /= dist;
					dz /= dist;
					this.xd += dx * 0.1;
					this.yd += dy * 0.12;
					this.zd += dz * 0.1;
					// Rotate to face target
					this.yRot = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
					this.xRot = (float) -(Math.atan2(dy, MathHelper.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
				}
			}

			this.move(this.xd, this.yd, this.zd);
			// Reduce friction for aerial movement
			float airFriction = 0.58F;
			this.xd *= airFriction;
			this.yd *= airFriction;
			this.zd *= airFriction;
		} else {
			// Terrestrial movement if not delivering
			super.moveEntityWithHeading(moveStrafing, moveForward);
		}

		// Update animation
		this.walkAnimSpeedO = this.walkAnimSpeed;
		double d = this.x - this.xo;
		double d1 = this.z - this.zo;
		float f4 = MathHelper.sqrt(d * d + d1 * d1) * 4.0F;
		if (f4 > 1.0F) {
			f4 = 1.0F;
		}
		this.walkAnimSpeed += (f4 - this.walkAnimSpeed) * 0.4F;
		this.walkAnimPos += this.walkAnimSpeed;

		// Show flying particles if delivering
		if (this.isDelivering && this.random.nextInt(10) == 0) {
//			this.world.spawnParticle("smoke", this.x + (this.random.nextFloat() - 0.5F) * this.bbWidth,
//				this.y + this.random.nextFloat() * this.bbHeight,
//				this.z + (this.random.nextFloat() - 0.5F) * this.bbWidth);
		}
	}

	public boolean canClimb() {
		return false;
	}

	@Override
	public boolean interact(@NotNull Player player) {
		super.interact(player);
		ItemStack playerItem = player.inventory.getCurrentItem();

		if (playerItem != null && playerItem.itemID == PaperworkItems.CLOSED_LETTER.id && this.itemStack == null) {
			// Pigeon takes the letter
			this.itemStack = playerItem.copy();
			this.setDelivering(true);
			playerItem.consumeItem(player);
			CompoundTag tag = this.itemStack.getData();
			this.tox = tag.containsKey("tox") ? tag.getInteger("tox") : 0;
			this.toy = tag.containsKey("toy") ? tag.getInteger("toy") : 0;
			this.toz = tag.containsKey("toz") ? tag.getInteger("toz") : 0;
			this.entityData.set(DATA_ITEM_STACK, this.itemStack);
			this.entityData.set(DATA_DEST_X, this.tox);
			this.entityData.set(DATA_DEST_Y, this.toy);
			this.entityData.set(DATA_DEST_Z, this.toz);
			LOGGER.info("Pigeon received letter with destination: ({}, {}, {})", tox, toy, toz);
			this.showDeliveryParticles(true);
			return true;
		} else if (playerItem == null && this.itemStack != null && player.isSneaking()) {
			// Player retrieves the letter
			player.inventory.insertItem(this.itemStack, true);
			this.itemStack = null;
			this.setDelivering(false);
			this.entityData.set(DATA_ITEM_STACK, null);
			this.pathToEntity = null;
			LOGGER.info("Player retrieved letter from pigeon");
			this.showDeliveryParticles(false);
			return true;
		}

		return false;
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		this.oFlap = this.flap;
		this.oFlapSpeed = this.flapSpeed;
		this.flapSpeed = (float)((double)this.flapSpeed + (double)(this.onGround ? -1 : 4) * 0.3);
		if (this.flapSpeed < 0.0F) {
			this.flapSpeed = 0.0F;
		}

		if (this.flapSpeed > 1.0F) {
			this.flapSpeed = 1.0F;
		}

		if (!this.onGround && this.flapping < 1.0F) {
			this.flapping = 1.0F;
		}

		this.flapping = (float)((double)this.flapping * 0.9);
		if (!this.onGround && this.yd < 0.0) {
			this.yd *= 0.6;
		}

		this.flap += this.flapping * 2.0F;
		if (this.isDelivering && this.itemStack != null) {
			// Reduce gravity for flight simulation
			if (!this.onGround) {
				if (this.yd < 0.0) {
					this.yd *= 0.6; // Slower fall
				} else {
					this.yd += 0.01; // Slight upward lift to maintain altitude
				}
			}
		} else {
			if (!this.onGround && this.yd < 0.0) {
				this.yd *= 0.6; // Normal gliding if not delivering
			}
		}
		if (this.isInWater()) {
			this.setDelivering(false);
			if (this.itemStack != null) {
				this.dropItem(this.itemStack, 0);
				this.itemStack = null;
				this.entityData.set(DATA_ITEM_STACK, null);
				LOGGER.warn("Pigeon dropped letter due to being in water");
			}
		}
	}

	@Override
	protected void updateAI() {
		super.updateAI();
		if (this.isDelivering && this.itemStack != null) {
			// Navigate to destination
			this.pathToEntity = this.world.getEntityPathToXYZ(this, tox, toy, toz, 32.0F);
			if (this.pathToEntity == null) {
				LOGGER.debug("No path found to ({}, {}, {}), distance limit: 32.0", tox, toy, toz);
				if (this.distanceTo(tox, toy, toz) > 16.0F) {
					teleportToDestination();
				}
			}
			if (this.distanceTo(tox, toy, toz) < 3.0F) {
				this.dropItem(this.itemStack, 0);
				this.itemStack = null;
				this.setDelivering(false);
				this.entityData.set(DATA_ITEM_STACK, null);
				this.pathToEntity = null;
				LOGGER.info("Pigeon delivered letter at ({}, {}, {})", tox, toy, toz);
				this.showDeliveryParticles(true);
			}
		}
	}

	private void teleportToDestination() {
		int targetX = MathHelper.floor(tox);
		int targetY = MathHelper.floor(toy);
		int targetZ = MathHelper.floor(toz);
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if ((Math.abs(x) > 1 || Math.abs(z) > 1) &&
					this.world.isBlockNormalCube(targetX + x, targetY - 1, targetZ + z) &&
					!this.world.isBlockNormalCube(targetX + x, targetY, targetZ + z) &&
					!this.world.isBlockNormalCube(targetX + x, targetY + 1, targetZ + z)) {
					this.moveTo(targetX + x + 0.5F, targetY, targetZ + z + 0.5F, this.yRot, this.xRot);
					this.fallDistance = 0.0F;
					LOGGER.info("Pigeon teleported to ({}, {}, {})", targetX + x, targetY, targetZ + z);
					return;
				}
			}
		}
		LOGGER.warn("Could not find valid teleport position near ({}, {}, {})", targetX, targetY, targetZ);
	}

	private void showDeliveryParticles(boolean isReceiving) {
		String particle = isReceiving ? "heart" : "smoke";
		for (int i = 0; i < 5; i++) {
			double motionX = this.random.nextGaussian() * 0.02;
			double motionY = this.random.nextGaussian() * 0.02;
			double motionZ = this.random.nextGaussian() * 0.02;
//			this.world.spawnParticle(particle, this.x + (this.random.nextFloat() * this.bbWidth * 2.0F) - this.bbWidth,
//				this.y + 0.5 + (this.random.nextFloat() * this.bbHeight),
//				this.z + (this.random.nextFloat() * this.bbWidth * 2.0F) - this.bbWidth,
//				motionX, motionY, motionZ, 0);
		}
	}

	@Override
	protected void causeFallDamage(float distance) {
		// No fall damage for pigeons
	}

	@Override
	protected boolean canDespawn() {
		return this.itemStack == null && super.canDespawn();
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.containsKey("Letter")) {
			CompoundTag letterTag = tag.getCompound("Letter");
			this.itemStack = ItemStack.readItemStackFromNbt(letterTag);
			this.entityData.set(DATA_ITEM_STACK, this.itemStack);
		} else {
			this.itemStack = null;
			this.entityData.set(DATA_ITEM_STACK, null);
		}
		this.tox = tag.getInteger("DestX");
		this.toy = tag.getInteger("DestY");
		this.toz = tag.getInteger("DestZ");
		this.setDelivering(tag.getBoolean("Delivering"));
		this.entityData.set(DATA_DEST_X, this.tox);
		this.entityData.set(DATA_DEST_Y, this.toy);
		this.entityData.set(DATA_DEST_Z, this.toz);
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (this.itemStack != null) {
			CompoundTag letterTag = new CompoundTag();
			this.itemStack.writeToNBT(letterTag);
			tag.putCompound("Letter", letterTag);
		}
		tag.putInt("DestX", this.tox);
		tag.putInt("DestY", this.toy);
		tag.putInt("DestZ", this.toz);
		tag.putBoolean("Delivering", this.isDelivering);
	}

	public boolean isDelivering() {
		return (this.entityData.getByte(DATA_DELIVERING) & 1) != 0;
	}

	public void setDelivering(boolean delivering) {
		this.isDelivering = delivering;
		byte data = this.entityData.getByte(DATA_DELIVERING);
		if (delivering) {
			this.entityData.set(DATA_DELIVERING, (byte) (data | 1));
		} else {
			this.entityData.set(DATA_DELIVERING, (byte) (data & -2));
		}
	}

	@Override
	protected float getSoundVolume() {
		return 0.3F;
	}
}
