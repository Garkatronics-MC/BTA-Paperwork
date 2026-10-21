package deus.paperwork.entity.office_chair;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.core.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class EntityOfficeChairRenderer extends EntityRenderer<Entity> {

	public EntityOfficeChairRenderer() {
		super(0.3f);

	}


	@Override
	public void render(@NotNull TessellatorGeneral tessellatorGeneral, @NonNull Entity entity, double v, double v1, double v2, float v3, float v4) {
		this.bindTexture("/assets/paperwork/textures/entity/office_chair/office_chair.png");

	}
}
