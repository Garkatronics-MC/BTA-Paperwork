package deus.paperwork;

import deus.paperwork.block.PaperworkBlocks;
import deus.paperwork.entities.pigeon.MobPigeon;
import deus.paperwork.item.PaperworkItems;
import deus.paperwork.util.PaperworkConfig;
import deus.utils.initializers.EntityInitializer;
import deus.utils.react.React;
import deus.utils.react.State;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.entity.EntityDispatcher;
import net.minecraft.core.entity.animal.MobDeer;
import net.minecraft.core.sound.SoundTypes;
import net.minecraft.core.util.collection.NamespaceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import turniplabs.halplibe.HalpLibe;
import turniplabs.halplibe.util.GameStartEntrypoint;


public class Paperwork implements ModInitializer, GameStartEntrypoint {
	public static final String MOD_ID = HalpLibe.registerMod("paperwork", true);

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	static {
		PaperworkConfig.makeConfig();
	}

	@Override
	public void onInitialize() {
		PaperworkBlocks.initialize();
		PaperworkItems.initialize();
		EntityInitializer.initialize("deus.paperwork");

		LOGGER.info("{} Core Initialized.", MOD_ID);


    }


	@Override
	public void beforeGameStart() {
		IconCoordinate ALERT = TextureRegistry.getTexture("paperwork:gui/sprites/alert");


		System.out.println(ALERT);
		// SoundTypes.loadSoundsJson(MOD_ID);
		// LOGGER.info("{} Sounds Initialized.", MOD_ID);
//
//		try {
//		} catch (Exception var2) {
//			LOGGER.warn("PainScale: Failed to fully initialize assets, some issue may occur!", var2);
//		}

		// Paperwork.LOGGER.info("AAAAAAA {}", DragonFly.loadEntityAnimations(MOD_ID,"pigeon").isLoop());
		//SoundHelper.addSound(MOD_ID, "material/paper/paper.wav" );
	}

	@Override
	public void afterGameStart() {

	}
}
