package deus.utils.initializers;

import deus.paperwork.Paperwork;
import deus.paperwork.core.mixin.IAEntityDispatcher;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.client.render.EntityRendererDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityDispatcher;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.useless.dragonfly.data.entity.mojang.EntityGeometryMojangData;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import turniplabs.halplibe.helper.ModelHelper;

public class EntityModelInitializer {

	@SuppressWarnings("unchecked")
	public static void initialize(String prefix, EntityRendererDispatcher dispatcher) {
		Reflections reflections = new Reflections(prefix);

		for (Class<?> clazz : reflections.getTypesAnnotatedWith(RegisterEntityRenderer.class)) {
			RegisterEntityRenderer annotation = clazz.getAnnotation(RegisterEntityRenderer.class);

			if (annotation != null && Entity.class.isAssignableFrom(clazz)) {
				try {
					EntityRenderer<?> rendererInstance;

					rendererInstance = annotation.renderer()
						.getDeclaredConstructor()
						.newInstance();

					dispatcher.assignRenderer((Class)clazz, (EntityRenderer) rendererInstance);

					EntityDispatcher.EntityDispatcherEntry<?> entry = EntityDispatcher.getInstance().entryForClass((Class) clazz);

					Paperwork.LOGGER.info("@Registered Renderer & Model for entity -> {}", clazz.getName());
				} catch (Exception e) {
					Paperwork.LOGGER.error("Failed to register Renderer & Model for entity -> {}", clazz.getSimpleName(), e);
				}
			}
		}
	}


}
