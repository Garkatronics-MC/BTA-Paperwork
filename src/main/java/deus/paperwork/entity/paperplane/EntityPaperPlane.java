package deus.paperwork.entity.paperplane;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.registry.PaperworkItems;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.player.gamemode.Gamemodes;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.Dimension;
import net.minecraft.core.world.World;
import net.minecraft.core.world.weather.Weathers;
import org.jetbrains.annotations.NotNull;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterEntityRenderer(renderer = PaperPlaneRenderer.class)
@RegisterEntity(id = "paperplane", name = "paperplane")
public class EntityPaperPlane extends Entity {
	public enum WetState {
		DRY(0),
		WET(100),
		VERY_WET(150);

		private final int atUnderRainTime;

		WetState(int lvl) {
			this.atUnderRainTime = lvl;
		}

		public int getAtUnderRainTime() {
			return atUnderRainTime;
		}

		public static WetState get(int atUnderRainTime) {
			if (atUnderRainTime >= VERY_WET.atUnderRainTime) {
				return VERY_WET;
			} else if (atUnderRainTime >= WET.atUnderRainTime) {
				return WET;
			} else {
				return DRY;
			}
		}
	}

	private DyeColor color = DyeColor.WHITE;
	private static final int DATA_WET_STATE = 1;
	private final int maxTimeUnderRain = 200;
	private int currentTimeUnderRain = 0;
	public WetState wetState = WetState.DRY;
	private final double DEFAULT_GRAVITY = 0.008F;
	private final double WET_GRAVITY = 0.028F;
	private double currentGravity = DEFAULT_GRAVITY;

	public EntityPaperPlane(World world) {
		super(world);
		this.setSize(0.5F, 0.5F);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(DATA_WET_STATE, currentTimeUnderRain, Integer.class);
	}


	protected void updateWet() {
		if (world.dimension.id != Dimension.NETHER.id && world.getWeatherManager().getCurrentWeather() == Weathers.OVERWORLD_RAIN) {
			if (currentTimeUnderRain < maxTimeUnderRain) {
				currentTimeUnderRain++;
			} else {
				currentTimeUnderRain = maxTimeUnderRain;
				remove();
			}
			wetState = WetState.get(currentTimeUnderRain);
			this.entityData.set(DATA_WET_STATE, wetState.atUnderRainTime);
			switch (wetState) {
				case DRY:
					currentGravity = DEFAULT_GRAVITY;
					break;
				case WET:
				case VERY_WET:
					currentGravity = WET_GRAVITY;
					break;
			}
		}
	}

	@Override
	public void tick() {
		super.tick();

		updateWet();

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.isInWater()) {
			this.move(this.xd, this.yd, this.zd);
			this.xd *= 0.8;
			this.yd *= 0.8;
			this.zd *= 0.8;
		} else if (this.isInLava()) {
			this.move(this.xd, this.yd, this.zd);
			this.xd *= 0.5;
			this.yd *= 0.5;
			this.zd *= 0.5;
		} else {
			float friction = 1.01F;
			if (this.onGround) {
				friction = 0.5460001F;
				int blockId = this.world.getBlockId(MathHelper.floor(this.x), MathHelper.floor(this.bb.minY) - 1, MathHelper.floor(this.z));
				if (blockId > 0) {
					friction = Blocks.blocksList[blockId].friction * 0.91F;
				}
			}


			this.yd -= currentGravity;

			this.move(this.xd, this.yd, this.zd);

			this.xd *= friction;
			this.yd *= friction;
			this.zd *= friction;

			if (Math.hypot(this.xd, this.zd) > 0.1) {
				int blockX = MathHelper.floor(this.x);
				int blockY = MathHelper.floor(this.y);
				int blockZ = MathHelper.floor(this.z);
				if (this.world.getBlockId(blockX, blockY, blockZ) != 0) {
					this.hurt(null, 5, DamageType.COMBAT);
				}
			}
		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return true;
	}


	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
		this.currentTimeUnderRain = compoundTag.getInteger("CurrentTimeUnderRain");
		this.wetState = WetState.get(currentTimeUnderRain);
		this.entityData.set(DATA_WET_STATE, wetState.atUnderRainTime);
		this.currentGravity = (wetState == WetState.DRY) ? DEFAULT_GRAVITY : WET_GRAVITY;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
		compoundTag.putInt("CurrentTimeUnderRain", this.currentTimeUnderRain);
	}

	@Override
	public boolean hurt(Entity entity, int damage, DamageType type) {
		if (!this.world.isClientSide && !this.removed) {

			this.markHurt();

			if (entity!=null) {
				world.playSoundAtEntity(null, entity, MOD_ID+":material.paper.jiggle",  1.5F, 1.5f);
			}

			if (entity instanceof Player && ((Player)entity).getGamemode() == Gamemodes.CREATIVE) {
				this.remove();
			} else {
				dropOnHurt();
				this.remove();
			}

			return true;
		}
		return true;
	}

	protected void dropOnHurt() {
		this.dropItem(PaperworkItems.PAPERPLANE.id, 1, 0.0F);
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}
	public WetState getWetState() {
		return WetState.get(this.entityData.getInt(DATA_WET_STATE));
	}

	public void setColor(DyeColor color) {
		this.color = color;
	}

	public DyeColor getColor() {
		return color;
	}
}
