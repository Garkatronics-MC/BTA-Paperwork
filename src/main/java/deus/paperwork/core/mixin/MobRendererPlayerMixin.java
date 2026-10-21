package deus.paperwork.core.mixin;

import deus.paperwork.entity.motion.CarriedEntity;
import net.minecraft.client.Minecraft;

import net.minecraft.client.render.EntityRendererDispatcher;
import net.minecraft.client.render.Lighting;
import net.minecraft.client.render.entity.MobRendererPlayer;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MobRendererPlayer.class, remap = false)
public class MobRendererPlayerMixin {
	@Inject(method = "drawHeldObject", at = @At("HEAD"), remap = false)
	public void test(Player player, float partialTick, CallbackInfo ci) {
		Object held = player.getHeldObject();

		if (held instanceof CarriedEntity carriedBlock) {

			TessellatorGeneral tessellator = GLRenderer.getTessellator();
			tessellator.setTranslation(0.0, 0.0, 0.0);

			Minecraft mc = Minecraft.getMinecraft();
			TextureRegistry.worldAtlas.bind();
			Lighting.disable();
			GL11.glPushMatrix();
			GL11.glBlendFunc(770, 771);
			GL11.glEnable(3042);
			GL11.glDisable(2884);
			if (mc.isFullbrightEnabled()) {
				GL11.glShadeModel(7425);
			} else {
				GL11.glShadeModel(7424);
			}

			GL11.glScalef(0.55F, -0.55F, 0.55F);
			GL11.glTranslatef(carriedBlock.getOffsetX(), carriedBlock.getOffsetY(), carriedBlock.getOffsetZ());

			Entity carried = carriedBlock.getCarried();
			if (carried != null) {
				EntityRendererDispatcher.instance.getRenderer(carried)
					.render(tessellator, carriedBlock.getCarried(), 0.0, 0.0, 0.0, 0.0f, partialTick);
			}

			GL11.glPopMatrix();
			GL11.glEnable(2896);
			GL11.glEnable(16384);
			GL11.glEnable(16385);
			GL11.glEnable(2903);
		}
	}


}
