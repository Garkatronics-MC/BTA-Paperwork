package deus.paperwork.entities.employee.farmer;

import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.uniforms.MobRendererUniformedDecorated;
import net.minecraft.client.render.entity.MobRendererBiped;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.useless.dragonfly.models.entity.StaticEntityModel;
public class MobEmployeeFarmerRenderer extends MobRendererUniformedDecorated<MobEmployeeFarmer> {

	private static final int BAG_LAYER = HAIR_LAYER + 1;

	public MobEmployeeFarmerRenderer() {
		super(0.3f);
	}

	@Override
	protected @Nullable StaticEntityModel getActiveModel(@NonNull MobEmployeeFarmer entity) {
		return this.getModel("main");
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobEmployeeFarmer entity,
		float brightness,
		float partialTick,
		int layer
	) {
		if (layer == BAG_LAYER) {
			return setupBagLayer(entity, partialTick);
		}

		StaticEntityModel model = super.getAndSetupModelForLayer(entity, brightness, partialTick, layer);
		if (model == null || layer != 0) return model;

		this.bindTexture("/assets/paperwork/textures/entity/employee/" + entity.getTextureReference() + ".png");

		boolean wheat1 = false, wheat2 = false, wheat3 = false;
		ItemStack stack = entity.inventory.findStackOf(Items.WHEAT);
		if (stack != null && stack.stackSize > 0) {
			wheat1 = true;
			wheat2 = stack.stackSize >= 32;
			wheat3 = stack.stackSize >= 64;
		}
		model.getTransform("wheat1").visible = wheat1;
		model.getTransform("wheat2").visible = wheat2;
		model.getTransform("wheat3").visible = wheat3;

		return model;
	}

	private @Nullable StaticEntityModel setupBagLayer(@NonNull MobEmployeeFarmer entity, float partialTick) {
		this.bindTexture("/assets/paperwork/textures/entity/employee/accessories/farmer_bag.png");
		StaticEntityModel model = this.getModel("farmer_bag");
		if (model == null) return null;
		return this.setupAnimations(entity, model, partialTick, BAG_LAYER);
	}

	@Override
	protected int maxRenderLayer(@NonNull MobEmployeeFarmer entity) {
		return BAG_LAYER;
	}
}
