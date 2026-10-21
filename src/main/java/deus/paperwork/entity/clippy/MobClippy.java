package deus.paperwork.entity.clippy;

import deus.paperwork.entity.base.MobPet;
import deus.paperwork.entity.donut.EntityDonutRenderer;
import deus.paperwork.registry.PaperworkItems;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.core.world.World;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterEntityRenderer(renderer = ClippyRenderer.class)
@RegisterEntity(id = "clippy", name = "clippy")
public class MobClippy extends MobPet {
	public MobClippy(World world) {
		super(world, PaperworkItems.IRON_NUGGET);
	}
}
