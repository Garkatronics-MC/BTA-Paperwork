package deus.paperwork.entity.big_paperplane;

import deus.paperwork.entity.paperplane.EntityPaperPlane;
import deus.paperwork.registry.PaperworkItems;
import deus.utils.annotations.RegisterEntity;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterEntity(id = "entity_big_paperplane", name = "entity_big_paperplane")
public class EntityBigPaperPlane extends EntityPaperPlane {

	public EntityBigPaperPlane(World world) {
		super(world);
		this.setSize(1F, 1F);
	}

	@Override
	public boolean canRide() {
		return true;
	}
	public double getRideHeight() {
		return (double)this.bbHeight - 0.7;
	}
	public void positionRider() {
		if (this.passenger != null) {
			double d = Math.cos((double)this.yRot * Math.PI / 180.0) * 0.1;
			double d1 = Math.sin((double)this.yRot * Math.PI / 180.0) * 0.1;
			this.passenger.setPos(this.x + d, this.y + this.getRideHeight() + this.passenger.getRidingHeight(), this.z + d1);
		}
	}

	@Override
	public void tick() {
		super.tick();

	}

	@Override
	public boolean interact(@NotNull Player player) {
		if (this.passenger != null && this.passenger instanceof Player && this.passenger != player) {
			return true;
		}
		if (!this.world.isClientSide) {
			player.startRiding(this);
		}
		return true;
	}

	@Override
	public boolean hurt(Entity entity, int damage, DamageType type) {
		if (this.passenger != null) {
			this.ejectRider();
		}
		return super.hurt(entity, damage, type);
	}

	@Override
	protected void dropOnHurt() {
		this.dropItem(PaperworkItems.BIG_PAPERPLANE.id, 1, 0.0F);
	}

	@Override
	public Entity ejectRider() {
		Entity entity = this.passenger;
		entity.fallDistance = 0;
		return super.ejectRider();
	}
}
