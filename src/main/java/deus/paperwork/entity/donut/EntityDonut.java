package deus.paperwork.entity.donut;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.entity.base.EntityFood;
import deus.paperwork.entity.pigeon.MobPigeonRenderer;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterEntityRenderer(renderer = EntityDonutRenderer.class)
@RegisterEntity(id = "donut", name = "donut")
public class EntityDonut extends EntityFood {
	public EntityDonut(@Nullable World world) {
		super(world);
		textureIdentifier = NamespaceID.fromPool(MOD_ID, "donut");
		sound = MOD_ID + ":entity.donut.eaten";
		setSize(0.3f, 0.3f);
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
