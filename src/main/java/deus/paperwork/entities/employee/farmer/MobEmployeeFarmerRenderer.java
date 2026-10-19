package deus.paperwork.entities.employee.farmer;

import deus.paperwork.entities.employee.MobEmployee;
import net.minecraft.client.render.entity.MobRendererBiped;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.useless.dragonfly.models.entity.StaticEntityModel;

public class MobEmployeeFarmerRenderer extends MobRendererBiped<MobEmployee> {
	public MobEmployeeFarmerRenderer() {
		super(0.3f);
	}

	@Override
	protected @Nullable StaticEntityModel getActiveModel(@NonNull MobEmployee mobEmployee) {
		return this.getModel(mobEmployee.isWoman() ? "woman" : "man");
	}



	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobEmployee entity,
		float brightness,
		float partialTick,
		int layer
	) {
		if(entity.isWoman()) {
			this.bindTexture("/assets/paperwork/textures/entity/employee_farmer/woman/" + entity.getTextureReference() + ".png");
		} else {
			this.bindTexture("/assets/paperwork/textures/entity/employee_farmer/" + entity.getTextureReference() + ".png");
		}

		StaticEntityModel model = super.getAndSetupModelForLayer(
			entity,
			brightness,
			partialTick,
			layer
		);

		if (model == null) {
			return null;
		}

		boolean wheat1 = false;
		boolean wheat2 = false;
		boolean wheat3 = false;

		ItemStack stack = entity.inventory.findStackOf(Items.WHEAT);

		if (stack != null && stack.stackSize > 0) {
			wheat1 = true;

			if (stack.stackSize >= 32) {
				wheat2 = true;
			}

			if (stack.stackSize >= 64) {
				wheat3 = true;
			}
		}

		model.getTransform("wheat1").visible = wheat1;
		model.getTransform("wheat2").visible = wheat2;
		model.getTransform("wheat3").visible = wheat3;

		return model;
	}




}
