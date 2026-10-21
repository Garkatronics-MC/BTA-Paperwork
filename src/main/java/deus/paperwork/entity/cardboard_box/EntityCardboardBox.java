package deus.paperwork.entity.cardboard_box;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import deus.paperwork.Paperwork;
import deus.paperwork.entity.motion.CarriedEntity;
import deus.paperwork.core.api.IItemWeight;
import deus.paperwork.registry.PaperworkItems;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.entity.TileEntityDispatcher;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityDispatcher;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.player.inventory.InventorySorter;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.util.HardIllegalArgumentException;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.primitives.AABBdc;

import java.lang.reflect.Constructor;
import java.util.Objects;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterEntityRenderer(renderer = RendererCardboardBox.class)
@RegisterEntity(id = "cardboard_box", name = "cardboard_box")
public class EntityCardboardBox extends Entity implements Container {

	private final double DEFAULT_GRAVITY = 0.1F;

	private ItemStack[] chestContents = new ItemStack[36];
	private BoxSize boxSize = BoxSize.LARGE;
	public boolean slotsLocked = false;
	private DyeColor color = null;
	public boolean halloween = false;
	private int savedBlockId = 0;
	private int savedBlockMeta = 0;
	private CompoundTag entityCompoundTag = null;
	private NamespaceID savedEntityNamespaceId = null;

	public EntityCardboardBox(@Nullable World world) {
		super(Objects.requireNonNull(world));
		this.setSize(0.8F, 0.8F);
	}

	public NamespaceID getSavedEntityNamespaceId() {
		return savedEntityNamespaceId;
	}

	public CompoundTag getEntityCompoundTag() {
		return entityCompoundTag;
	}

	public BoxSize getBoxSize() {
		return boxSize;
	}

	public boolean isLocked() {
		return savedEntityNamespaceId != null || savedBlockId != 0;
	}

	public DyeColor getColor() {
		return color;
	}

	public void setBoxSize(BoxSize boxSize) {
		this.boxSize = boxSize;
		this.setSize(boxSize.getSize(), boxSize.getSize());

		ItemStack[] newContents = new ItemStack[getContainerSize()];
		for (int i = 0; i < Math.min(newContents.length, chestContents.length); i++) {
			newContents[i] = chestContents[i];
		}
		chestContents = newContents;
	}

	public void setSavedBlockId(int savedBlockId) {
		this.savedBlockId = savedBlockId;
	}

	public void setSavedBlockMeta(int savedBlockMeta) {
		this.savedBlockMeta = savedBlockMeta;
	}

	public int getSavedBlockId() {
		return savedBlockId;
	}

	public int getSavedBlockMeta() {
		return savedBlockMeta;
	}

	public void setTileEntity(TileEntity tileEntity) {
		this.entityCompoundTag = new CompoundTag();
		tileEntity.writeToNBT(entityCompoundTag);
		savedEntityNamespaceId = TileEntityDispatcher.getIDFromClass(tileEntity.getClass());
	}


	public double getMaxWeight() {
		return boxSize.getMaxWeight();
	}

	public int getContainerSize() {
		return boxSize.getSlots();
	}

	public String getNameTranslationKey() {
		return "container.chest.name";
	}

	public int getMaxStackSize() {
		return 64;
	}

	@Override
	protected void defineSynchedData() {
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
		ListTag nbttaglist = compoundTag.getList("Items");
		this.chestContents = new ItemStack[this.getContainerSize()];
		for (int i = 0; i < nbttaglist.tagCount(); ++i) {
			CompoundTag itemTag = (CompoundTag) nbttaglist.tagAt(i);
			int slot = itemTag.getByte("Slot") & 255;
			if (slot >= 0 && slot < this.chestContents.length) {
				this.chestContents[slot] = ItemStack.readItemStackFromNbt(itemTag);
			}
		}

		if (compoundTag.containsKey("BoxSize")) {
			this.setBoxSize(BoxSize.values()[compoundTag.getInteger("BoxSize")]);
		}

		savedBlockId = compoundTag.getInteger("SavedBlockID");
		savedBlockMeta = compoundTag.getInteger("SavedBlockMeta");

		halloween = compoundTag.getBoolean("Halloween");

		if (compoundTag.containsKey("EntityCompoundTag")) {
			entityCompoundTag = compoundTag.getCompound("EntityCompoundTag");
		}

		if (compoundTag.containsKey("SavedEntityNamespace")) {
			try {
				savedEntityNamespaceId = NamespaceID.getPermanent(compoundTag.getString("SavedEntityNamespace"));
				Paperwork.LOGGER.info("Entity loaded: {}", savedEntityNamespaceId);
			} catch (HardIllegalArgumentException e) {
				throw new RuntimeException(e);
			}
		}

		if (compoundTag.containsKey("Color")) {
			color = DyeColor.colorFromItemMeta(compoundTag.getInteger("Color"));
		}
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
		compoundTag.put("Items", saveItems());
		compoundTag.putInt("BoxSize", this.getBoxSize().ordinal());
		compoundTag.putInt("SavedBlockID", savedBlockId);
		compoundTag.putInt("SavedBlockMeta", savedBlockMeta);
		compoundTag.putBoolean("Halloween", halloween);

		if (color != null) {
			compoundTag.putInt("Color", color.itemMeta);
		}

		if(entityCompoundTag != null) {
			compoundTag.putCompound("EntityCompoundTag", entityCompoundTag);
		}

		if (savedEntityNamespaceId != null) {
			Paperwork.LOGGER.info("Entity saved: {}", savedEntityNamespaceId);
			compoundTag.putString("SavedEntityNamespace", savedEntityNamespaceId.toString());
		}
	}

	private ListTag saveItems() {
		ListTag nbttaglist = new ListTag();
		for (int i = 0; i < this.chestContents.length; ++i) {
			if (this.chestContents[i] != null) {
				CompoundTag nbttagcompound1 = new CompoundTag();
				nbttagcompound1.putByte("Slot", (byte) i);
				this.chestContents[i].writeToNBT(nbttagcompound1);
				nbttaglist.addTag(nbttagcompound1);
			}
		}
		return nbttaglist;
	}


	private void updateWeightStatus() {
		// slotsLocked = gravity < boxSize.getMaxWeight();
	}

	private double calcGravity() {
		double totalWeight = 0;
		if (chestContents == null) return 0;

		for (int i = 0; i < Math.min(chestContents.length, getContainerSize()); i++) {
			ItemStack stack = chestContents[i];
			if (stack != null && stack.getItem() instanceof IItemWeight) {
				totalWeight += stack.stackSize * ((IItemWeight) stack.getItem()).paperwork$getWeight();
			}
		}
		return totalWeight;
	}

	@Override
	public void tick() {
		super.tick();
		updateWeightStatus();

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.isInWater()) {
			double buoyancy = 0.05 - calcGravity();
			this.yd += buoyancy;
			this.move(this.xd, this.yd, this.zd);
			this.xd *= 0.9;
			this.yd *= 0.9;
			this.zd *= 0.9;
		} else if (this.isInLava()) {
			this.move(this.xd, this.yd, this.zd);
			this.xd *= 0.5;
			this.yd *= 0.5;
			this.zd *= 0.5;
		} else {
			float friction = this.onGround ? 0.5460001F : 1.01F;
			if (this.onGround) {
				int blockId = this.world.getBlockId(MathHelper.floor(this.x), MathHelper.floor(this.bb.minY) - 1, MathHelper.floor(this.z));
				if (blockId > 0) friction = Blocks.blocksList[blockId].friction * 0.91F;
			}

			this.yd -= DEFAULT_GRAVITY + calcGravity();
			this.move(this.xd, this.yd, this.zd);
			this.xd *= friction;
			this.yd *= friction;
			this.zd *= friction;

			if (Math.hypot(this.xd, this.zd) > 0.1) {
				int blockX = MathHelper.floor(this.x);
				int blockY = MathHelper.floor(this.y);
				int blockZ = MathHelper.floor(this.z);
				if (this.world.getBlockId(blockX, blockY, blockZ) != 0) {
					this.hurt(null, 5, DamageType.COMBAT);
				}
			}
		}
	}

	@Override
	public boolean interact(@NotNull Player player) {
		if (player.isSneaking() && player.getHeldObject() == null && player.getHeldItem() == null) {
			player.setHeldObject(new CarriedEntity(player, this).setXYZ(boxSize.getX(), boxSize.getY(), boxSize.getZ()));
			remove();
		} else {
			ItemStack itemStack = player.getCurrentEquippedItem();

			if (itemStack != null && (itemStack.itemID == Items.TOOL_SHEARS.id || itemStack.itemID == Items.TOOL_SHEARS_STEEL.id)) {
				dropAdditionalContent();
				halloween = false;
				color = null;
				return true;
			}

			if (itemStack != null && itemStack.itemID == Blocks.PUMPKIN_CARVED_IDLE.asItem().id) {
				halloween = !halloween;
				return true;
			}

			if (itemStack != null && itemStack.itemID == Items.PAPER.id) {
				CompoundTag data = itemStack.getData();
				if (data.containsKey("Color")) {
					color = DyeColor.colorFromItemMeta(data.getInteger("Color"));
					itemStack.consumeItem(player);
				}
			} else {
				// ((IPaperworkDisplay) player).paperwork$displayCardboardBoxScreen(this);
			}
		}
		return true;
	}

	public void dropAdditionalContent() {
		if (halloween) dropItem(Blocks.PUMPKIN_CARVED_IDLE.id(), 1);

		if (color != null) {
			ItemStack c = new ItemStack(Items.PAPER, 1);
			CompoundTag data = c.getData();
			data.putInt("Color", color.itemMeta);
			c.setData(data);
			dropItem(c, 0);
		}
	}

	@Override
	public boolean hurt(Entity attacker, int baseDamage, DamageType type) {
		if (attacker instanceof Player || type == DamageType.DROWN || type == DamageType.FIRE) {
			ItemStack stack = null;

			switch (boxSize) {
				case SMALL: stack =  new ItemStack(PaperworkItems.CARDBOARD_BOX_SMALL); break;
				case MEDIUM: stack =  new ItemStack(PaperworkItems.CARDBOARD_BOX_MEDIUM); break;
				case REGULAR: stack =  new ItemStack(PaperworkItems.CARDBOARD_BOX_REGULAR); break;
				case LARGE: stack =  new ItemStack(PaperworkItems.CARDBOARD_BOX_LARGE); break;
			}

			if (this.getSavedBlockId() != 0) {
				world.setBlockAndMetadataWithNotify((int) x, (int) y, (int) z, this.getSavedBlockId(), this.getSavedBlockMeta());

				if (savedEntityNamespaceId != null) {
					TileEntity newTileEntity = TileEntityDispatcher.createAndLoadEntity(entityCompoundTag);
					world.setTileEntity((int) x, (int) y, (int) z, newTileEntity);
				}
			} else if(savedEntityNamespaceId != null) {
				// Constructor<? extends Entity> constructor = EntityDispatcher.classForId(savedEntityNamespaceId).getDeclaredConstructor(World.class);
				//constructor.setAccessible(true);
				// Entity newEntity = constructor.newInstance(world);

//					newEntity.readAdditionalSaveData(this.entityCompoundTag);
//					newEntity.moveTo(x + 0.5, y, z + 0.5, 0, 0);
//					newEntity.spawnInit();
//					world.entityJoinedWorld(newEntity);
			}

			dropAdditionalContent();
			dropItem(stack, 0);
			dropContents(world, (int) x, (int) y, (int) z);
			this.remove();
			return super.hurt(attacker, baseDamage, type);
		}
		return false;
	}

	public @Nullable ItemStack getItem(int index) {
		return this.chestContents[index];
	}

	public void setItem(int index, @Nullable ItemStack itemstack) {
		this.chestContents[index] = itemstack;
		if (itemstack != null && itemstack.stackSize > this.getMaxStackSize()) {
			itemstack.stackSize = this.getMaxStackSize();
		}
		this.setChanged();
	}

	public @Nullable ItemStack removeItem(int index, int takeAmount) {
		if (this.chestContents[index] != null) {
			ItemStack itemstack1;
			if (this.chestContents[index].stackSize <= takeAmount) {
				itemstack1 = this.chestContents[index];
				this.chestContents[index] = null;
				this.setChanged();
				return itemstack1;
			} else {
				itemstack1 = this.chestContents[index].splitStack(takeAmount);
				if (this.chestContents[index].stackSize <= 0) {
					this.chestContents[index] = null;
				}
				this.setChanged();
				return itemstack1;
			}
		}
		return null;
	}

	public void sortContainer() {
		InventorySorter.sortInventory(this.chestContents);
	}

	public boolean stillValid(Player entityplayer) {
		return true;
	}

	@Override
	public void sort() {

	}

	@Override
	public boolean locked(int slot) {
		return Container.super.locked(slot);
	}

	@Override
	public void setChanged() {
		updateWeightStatus();
	}

	public void dropContents(World world, int x, int y, int z) {
		for (int i = 0; i < this.getContainerSize(); ++i) {
			ItemStack itemStack = this.getItem(i);
			if (itemStack != null) {
				EntityItem item = world.dropItem(x, y, z, itemStack);
				item.xd *= 0.5;
				item.yd *= 0.5;
				item.zd *= 0.5;
				item.pickupDelay = 0;
			}
		}
	}

	public void setEntity(Entity entity) {
//		this.entityCompoundTag = new CompoundTag();
//		entity.addAdditionalSaveData(entityCompoundTag);
//		savedEntityNamespaceId = EntityDispatcher.idForClass(entity.getClass());
//		savedEntityNamespaceId.makePermanent();
	}

	@Override
	public @Nullable AABBdc getCollisionAABB() {
		return bb;
	}
}
