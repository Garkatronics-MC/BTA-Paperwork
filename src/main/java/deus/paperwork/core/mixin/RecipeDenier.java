package deus.paperwork.core.mixin;


import net.minecraft.core.data.registry.recipe.RecipeEntryBase;
import net.minecraft.core.data.registry.recipe.RecipeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import static deus.paperwork.Paperwork.LOGGER;


@Mixin(
	value = {RecipeRegistry.class},
	remap = false
)
public class RecipeDenier {
	@Unique private final static List<String> toDeny = new ArrayList<>();

	static {
//		toDeny.add("minecraft:workbench/paper");
	}

	@Inject(method = "addCustomRecipe", at = @At("HEAD"), cancellable = true)
	private void RecipeDenierInject(String recipeKey, RecipeEntryBase<?, ?, ?> recipe, CallbackInfo ci) {
		if (toDeny.contains(recipeKey)) {
			LOGGER.info("Recipe {} is denied in Mixin", recipeKey);
			ci.cancel();
		}
	}
}
