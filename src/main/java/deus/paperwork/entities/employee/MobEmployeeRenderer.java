package deus.paperwork.entities.employee;

import deus.paperwork.uniforms.MobRendererUniformedDecorated;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.entity.MobRendererBiped;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.core.Global;
import net.minecraft.core.entity.SkinVariantList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.useless.dragonfly.models.entity.StaticEntityModel;

public class MobEmployeeRenderer extends MobRendererUniformedDecorated<MobEmployee> {
	public MobEmployeeRenderer() {
		super(0.3f);
	}

	@Override
	protected @Nullable StaticEntityModel getActiveModel(@NonNull MobEmployee mobEmployee) {
		return this.getModel("main");
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(@NonNull MobEmployee entity, float brightness, float partialTick, int layer) {

		this.bindTexture("/assets/paperwork/textures/entity/employee/" + entity.getTextureReference() + ".png");

		return super.getAndSetupModelForLayer(entity, brightness, partialTick, layer);
	}
}
