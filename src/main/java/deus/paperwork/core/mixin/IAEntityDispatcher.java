package deus.paperwork.core.mixin;

import net.minecraft.client.render.EntityRendererDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = EntityRendererDispatcher.class, remap = false)
public interface IAEntityDispatcher {

	@Accessor("renderers")
	Map<Class<?>, EntityRenderer<?>> getRenderers();

}
