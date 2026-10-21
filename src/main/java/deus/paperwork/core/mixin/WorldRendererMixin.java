package deus.paperwork.core.mixin;

import deus.paperwork.core.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldRenderer.class, remap = false)
public abstract class WorldRendererMixin {

	@Shadow
	public Minecraft mc;
	@Shadow
	private long systemTime;

	@Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/util/debug/Debug;change(Ljava/lang/String;)V", ordinal = 9))
	public void renderInject(float partialTick, long updateRenderersUntil, CallbackInfo ci){
		RenderUtils.renderInfo(this.mc, partialTick, this.systemTime);
	}
}
