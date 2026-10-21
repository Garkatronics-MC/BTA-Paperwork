package deus.paperwork.entity.motion;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.Paperwork;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityDispatcher;
import net.minecraft.core.util.HardIllegalArgumentException;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.ICarriable;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;

public class CarriedEntity implements ICarriable {
	private static final String TYPE_ENTITY = "entity";
	private float offsetX = 0.0F;
	private float offsetY = -1.61F;
	private float offsetZ = -0.75F;
	private @Nullable Entity carried;
	private @NotNull Entity holder;
	private @NotNull World world;
	private NamespaceID savedEntityNamespaceId = null;
	private @Nullable CompoundTag cachedEntityData;

	public CarriedEntity(@NotNull Entity holder, @NotNull Entity carried) {
		this.holder = holder;
		this.carried = carried;
		Paperwork.LOGGER.info("[CarriedEntity]: Carried -> {}", carried);
		this.world = holder.world;
		if (this.world == null) {
			throw new IllegalStateException("World reference must not be null!");
		}
		this.cachedEntityData = new CompoundTag();
		this.carried.addAdditionalSaveData(this.cachedEntityData);
		// savedEntityNamespaceId = EntityDispatcher.idForClass(carried.getClass());
		savedEntityNamespaceId.makePermanent();
	}

	private CarriedEntity(@NotNull Entity holder) {
		this.holder = holder;
		this.world = holder.world;
		Paperwork.LOGGER.info("[CarriedEntity]: Not carried entity inside");
		if (this.world == null) {
			throw new IllegalStateException("World reference must not be null!");
		}
	}

	@Override
	public void heldTick(World world, Entity holder) {
		if (this.carried != null) {
			this.carried.tick();
		}
	}

	@Override
	public boolean tryPlace(World world, Entity holder, int blockX, int blockY, int blockZ, Side side, double xPlaced, double yPlaced) {
		if (world.isClientSide) return false;

		Entity newEntity = recreateEntity();
		if (newEntity != null) {
			double px = blockX + side.offsetX() + 0.5;
			double py = blockY + side.offsetY();
			double pz = blockZ + side.offsetZ() + 0.5;

			newEntity.moveTo(px, py, pz, holder.yRot, holder.xRot);
			newEntity.spawnInit();
			world.entityJoinedWorld(newEntity);
			return true;
		}
		Paperwork.LOGGER.error("[CarriedEntity(tryPlace)]: newEntity is null.");
		return false;
	}

	@Override
	public void drop(World world, Entity holder) {
		int baseX = MathHelper.floor(holder.x);
		int baseY = MathHelper.floor(holder.y);
		int baseZ = MathHelper.floor(holder.z);

		if (tryPlace(world, holder, baseX, baseY, baseZ, Side.TOP, 0.0, 0.0)) {
			return;
		}

		for (int y = baseY - 1; y <= baseY + 1; y++) {
			for (int x = baseX - 1; x <= baseX + 1; x++) {
				for (int z = baseZ - 1; z <= baseZ + 1; z++) {
					if (x == baseX && y == baseY && z == baseZ) continue;
					if (tryPlace(world, holder, x, y, z, Side.TOP, 0.0, 0.0)) {
						return;
					}
				}
			}
		}

		Entity newEntity = recreateEntity();
		if (newEntity != null) {
			newEntity.moveTo(holder.x, holder.y, holder.z, 0, 0);
			newEntity.spawnInit();
			world.entityJoinedWorld(newEntity);
			Paperwork.LOGGER.info("[CarriedEntity(drop)]: Entity dropped at holder's position.");
		} else {
			Paperwork.LOGGER.warn("[CarriedEntity(drop)]: Failed to drop entity, recreation failed.");
		}
	}




	@Override
	public void writeToNBT(CompoundTag tag) {
		// ? If exists a entity ID, save it
		if (savedEntityNamespaceId != null) {
			Paperwork.LOGGER.info("Entity saved: {}", savedEntityNamespaceId);
			tag.putString("SavedEntityNamespace", savedEntityNamespaceId.toString());
		}

		// ? Save carried entity data
		if (cachedEntityData != null) {
			tag.putCompound("entity",this.cachedEntityData);
		}

		// ? Mark as type entity
		tag.putString("type", TYPE_ENTITY);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		// ? Avoid load other NBT data
		if (!TYPE_ENTITY.equals(tag.getString("type"))) {
			Paperwork.LOGGER.error("[CarriedEntity]: Invalid type in NBT: {}. Expected: {}.", tag.getString("type"), TYPE_ENTITY);
			return;
		}

		// ? Save entity ID
		if (tag.containsKey("SavedEntityNamespace")) {
			try {
				savedEntityNamespaceId = NamespaceID.getPermanent(tag.getString("SavedEntityNamespace"));
				Paperwork.LOGGER.info("Entity loaded: {}", savedEntityNamespaceId);
			} catch (HardIllegalArgumentException e) {
				throw new RuntimeException(e);
			}
		}

		// ? If exists an entity inside, read the NBT of it
		if (tag.containsKey("entity")) {
			this.cachedEntityData = tag.getCompound("entity");
			this.carried = recreateEntity();
			if (this.carried == null) {
				Paperwork.LOGGER.error("[CarriedEntity]: Failed to load entity from NBT data.");
			}
		} else {
			Paperwork.LOGGER.warn("[CarriedEntity]: No entity data found in NBT.");
		}
	}

	public static CarriedEntity createAndLoad(@NotNull Entity holder, CompoundTag tag) {
		CarriedEntity carriedEntity = new CarriedEntity(holder);
		carriedEntity.readFromNBT(tag);
		return carriedEntity;
	}

	@Nullable
	private Entity recreateEntity() {
//		try {
//			Constructor<? extends Entity> constructor = EntityDispatcher.classForId(savedEntityNamespaceId).getDeclaredConstructor(World.class);
//			constructor.setAccessible(true);
//			Entity newEntity = constructor.newInstance(world);
//			newEntity.readAdditionalSaveData(this.cachedEntityData);
//			return newEntity;
//
//		} catch (ReflectiveOperationException e) {
//			Paperwork.LOGGER.error("[" + getClass().getSimpleName() + "] Error instancing entity...");
//			return null;
//		}
		return  null;
	}



	public CarriedEntity setXYZ(float offsetX, float offsetY, float offsetZ) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		return this;
	}

	public float getOffsetX() {
		return offsetX;
	}

	public float getOffsetY() {
		return offsetY;
	}

	public float getOffsetZ() {
		return offsetZ;
	}

	@Nullable
	public Entity getCarried() {
		return carried;
	}
}
