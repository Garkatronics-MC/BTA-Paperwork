package deus.paperwork;

import deus.paperwork.registry.PaperworkBlocks;
import deus.paperwork.entity.employee.farmer.FarmerJob;
import deus.paperwork.entity.employee.job.EmployeeJobs;
import deus.paperwork.registry.PaperworkItems;
import deus.paperwork.core.util.PaperworkConfig;
import deus.utils.initializers.EntityInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
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
		// The farmer is a job: it is registered here (once, on both the client
		// and the server) instead of being a class of mob.
		EmployeeJobs.register(new FarmerJob());
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
