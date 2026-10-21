package deus.paperwork.entity.stapler_projectile;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;


public class StaplerProjectileRenderer extends EntityRenderer<StaplerProjectile> {



	@Override
	public void render(@NotNull TessellatorGeneral tessellatorGeneral, @NonNull StaplerProjectile entity,  double x, double y, double z, float yaw, float partialTick) {
		this.bindTexture("/assets/paperwork/textures/entity/iron_nugget.png");

		GL11.glPushMatrix();

		GL11.glTranslatef((float) x, (float) y, (float) z);

		GL11.glRotatef(entity.yRotO + (entity.yRot - entity.yRotO) * partialTick - 90.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(entity.xRotO + (entity.xRot - entity.xRotO) * partialTick, 0.0F, 0.0F, 1.0F);

		float scale = 0.5F;
		GL11.glScalef(scale, scale, scale);

		GL11.glDisable(GL11.GL_CULL_FACE);

		float u0 = 0.0F;
		float v0 = 0.0F;
		float u1 = 1.0F;
		float v1 = 1.0F;

//		tessellator.startDrawingQuads();
//		tessellator.addVertexWithUV(-0.5, -0.5, 0.0, u0, v1);
//		tessellator.addVertexWithUV( 0.5, -0.5, 0.0, u1, v1);
//		tessellator.addVertexWithUV( 0.5,  0.5, 0.0, u1, v0);
//		tessellator.addVertexWithUV(-0.5,  0.5, 0.0, u0, v0);
//		tessellator.draw();

		GL11.glEnable(GL11.GL_CULL_FACE);

		GL11.glPopMatrix();
	}
}
