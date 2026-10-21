package deus.paperwork.entity.office_chair;

import deus.paperwork.entity.base.EntityChair;
import deus.utils.annotations.RegisterEntity;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.Nullable;
import static deus.paperwork.Paperwork.MOD_ID;

@RegisterEntity(id = "office_chair", name = "office_chair")
public class EntityOfficeChair extends EntityChair {
	public EntityOfficeChair(@Nullable World world) {
		super(world);
		this.setSize(0.4F, 0.8F);
	}
}
