package deus.paperwork;

import deus.utils.initializers.EntityModelInitializer;
import deus.utils.initializers.ItemModelInitializer;
import net.minecraft.client.render.EntityRendererDispatcher;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import net.minecraft.client.render.block.color.BlockColorDispatcher;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import turniplabs.halplibe.util.ModelEntrypoint;

public class PaperworkModelEntrypoint implements ModelEntrypoint {
	@Override
	public void initBlockModels(BlockModelDispatcher dispatcher) {

	}

	@Override
	public void initItemModels(ItemModelDispatcher dispatcher) {
		ItemModelInitializer.initialize("deus.paperwork.item", dispatcher);
	}

	@Override
	public void initEntityModels(EntityRendererDispatcher dispatcher) {
		Paperwork.LOGGER.info("Initializing models...");
		EntityModelInitializer.initialize("deus.paperwork.entity", dispatcher);

	}

	@Override
	public void initTileEntityModels(TileEntityRenderDispatcher dispatcher) {}

	@Override
	public void initBlockColors(BlockColorDispatcher dispatcher) {

	}
}
