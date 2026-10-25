package deus.paperwork.entity.employee.guard;

import deus.paperwork.entity.employee.MobEmployee;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.monster.Enemy;
import net.minecraft.core.util.helper.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.primitives.AABBd;

import java.util.Optional;

/// Where a guard looks for a fight: the hostile mobs around it.
///
/// A hostile mob is one that implements BTA's {@link Enemy} marker (zombies,
/// skeletons, creepers, spiders, ...). Players are never targets, and neither
/// are other employees or passive animals.
///
/// The search is a plain world query with a box around the mob: cheap enough
/// to run every tick and with no side effects.
public final class GuardTargeting {

	/// How far (in blocks) a guard looks for a new hostile mob.
	public static final double DETECTION_RADIUS = 24.0;

	/// How far a guard keeps chasing the target it already has before it gives
	/// up and looks for another one. Larger than the detection radius so the
	/// guard does not drop a mob that is running away.
	public static final double LEASH_RADIUS = 32.0;

	/// Vertical reach of the search box.
	private static final double VERTICAL_REACH = 8.0;

	private GuardTargeting() {}

	/// The nearest hostile mob around the guard, empty when there is none.
	public static Optional<Entity> findNearestHostile(MobEmployee mob) {
		if (mob == null || mob.world == null) {
			return Optional.empty();
		}

		AABBd box = MathHelper.aabbGrow(
			mob.bb,
			DETECTION_RADIUS,
			VERTICAL_REACH,
			DETECTION_RADIUS,
			new AABBd()
		);

		Entity nearest = null;
		double nearestDistance = Double.MAX_VALUE;

		for (Entity candidate : mob.world.getEntitiesWithinAABB(Entity.class, box)) {
			if (!isHostile(candidate)) {
				continue;
			}

			double distance = candidate.distanceToSqr(mob);
			if (distance > DETECTION_RADIUS * DETECTION_RADIUS) {
				continue;
			}
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = candidate;
			}
		}

		return Optional.ofNullable(nearest);
	}

	/// Whether the given entity is a living hostile mob.
	public static boolean isHostile(@Nullable Entity entity) {
		return entity instanceof Enemy && entity.isAlive() && !entity.isRemoved();
	}

	/// Whether the guard should keep the target it already has instead of
	/// looking for a new one. A dead (or removed) target is always dropped, and
	/// so is one that has run beyond the leash.
	public static boolean canKeepTarget(MobEmployee mob, @Nullable Entity target) {
		if (mob == null || target == null || !isHostile(target)) {
			return false;
		}
		return target.distanceToSqr(mob) <= LEASH_RADIUS * LEASH_RADIUS;
	}
}
