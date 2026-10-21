package deus.paperwork.entity.stapler_projectile;

import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.projectile.Projectile;
import net.minecraft.core.world.World;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterEntityRenderer(renderer = StaplerProjectileRenderer.class)
@RegisterEntity(name = "entity_stapler_projectile", id = "entity_stapler_projectile")
public class StaplerProjectile extends Projectile {
	public StaplerProjectile(World world, Mob owner) {
		super(world, owner);
	}
	public StaplerProjectile(World world, double x, double y, double z) {
		super(world, x, y, z);
	}


}
