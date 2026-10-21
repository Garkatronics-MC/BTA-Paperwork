package deus.paperwork.entity.base;

import deus.paperwork.registry.PaperworkItems;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public abstract class EntityFood extends TexturedEntity {
	protected String sound = "";
	public EntityFood(@Nullable World world) {
		super(world);
	}

	@Override
	public void tick() {
		super.tick();

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		float friction = 0.54F;

		if (this.isInWater()) {
			friction = 0.8F;
		} else if (this.isInLava()) {
			friction = 0.5F;
		} else if (this.onGround) {
			int blockId = this.world.getBlockId(MathHelper.floor(this.x), MathHelper.floor(this.bb.minY) - 1, MathHelper.floor(this.z));
			if (blockId > 0) {
				friction = (Blocks.blocksList[blockId].friction * 0.91F);
			} else {
				friction = 0.546F;
			}
		}

		this.yd -= 0.4;

		this.xd *= friction;
		this.yd *= friction;
		this.zd *= friction;

		this.move(this.xd, this.yd, this.zd);
	}

	@Override
	public boolean interact(@NotNull Player player) {
		Random random = new Random();
		float pitch = 0.8F + random.nextFloat() * 0.4F;
		if (!sound.isEmpty()) world.playSoundAtEntity(null, player, sound, 1.0F, pitch);
		player.eatFood(new ItemStack(PaperworkItems.FOOD_DONUT));
		this.removed = true;

		return true;
	}



	@Override
	public boolean canInteract() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public boolean isPickable() {
		return true;
	}
}
