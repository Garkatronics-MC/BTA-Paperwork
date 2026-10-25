package deus.paperwork.entity.employee.guard;

import deus.paperwork.entity.employee.MobEmployee;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.monster.Enemy;
import net.minecraft.core.entity.monster.MobCreeper;
import net.minecraft.core.util.helper.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.primitives.AABBd;

import java.util.Optional;

/// Cómo busca y pelea el guardia: qué hostile considerar objetivo, a qué
/// distancia quiere estar según el tipo de mob y si puede conservar el
/// objetivo que ya persigue.
public final class GuardTargeting {

	/// Radio de detección de un nuevo hostil (bloques).
	public static final double DETECTION_RADIUS = 24.0;

	/// Radio máximo antes de abandonar la persecución (bloques).
	public static final double LEASH_RADIUS = 36.0;

	/// Distancia preferida para luchar contra un creeper: se queda más lejos
	/// para evitar la explosión.
	public static final double CREEPER_KEEP_DISTANCE = 8.0;

	/// Distancia preferida contra un hostil normal (bloques).
	public static final double NORMAL_KEEP_DISTANCE = 5.0;

	/// Alcance del ataque a distancia con el arco (bloques).
	public static final double BOW_REACH = 10.0;

	/// Alcance del ataque cuerpo a cuerpo con espada (bloques).
	public static final double SWORD_REACH = 3.0;

	/// Alcance del ataque a puñetazos (sin arma).
	public static final double FIST_REACH = 2.5;

	private static final double VERTICAL_REACH = 8.0;

	private GuardTargeting() {}

	/// El hostil más cercano alrededor del guardia.
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

	/// Si el objetivo ya perseguido sigue válido y dentro del lazo.
	public static boolean canKeepTarget(MobEmployee mob, @Nullable Entity target) {
		if (mob == null || target == null || !isHostile(target)) {
			return false;
		}
		return target.distanceToSqr(mob) <= LEASH_RADIUS * LEASH_RADIUS;
	}

	/// Si el mob es un creeper.
	public static boolean isCreeper(@Nullable Entity entity) {
		return entity instanceof MobCreeper;
	}

	/// A qué distancia quiere mantenerse el guardia del objetivo.
	///
	/// Un creeper siempre obliga a distancia de seguridad. Con arco se prefiere
	/// distancia de disparo; con espada o sin arma, distancia de cuerpo a cuerpo.
	public static double preferredDistance(MobEmployee mob, Entity target, GuardWeapon weapon) {
		if (isCreeper(target)) {
			return CREEPER_KEEP_DISTANCE;
		}
		return switch (weapon) {
			case BOW -> BOW_REACH;
			case SWORD -> SWORD_REACH;
			default -> FIST_REACH;
		};
	}

	/// Si el guardia está lo suficientemente cerca del objetivo para atacar
	/// según su arma y el tipo de mob.
	public static boolean inAttackRange(MobEmployee mob, Entity target, GuardWeapon weapon) {
		double distance = target.distanceTo(mob);
		return distance <= preferredDistance(mob, target, weapon);
	}

	/// Si el objetivo está demasiado cerca de un creeper y debe alejarse.
	public static boolean tooCloseToCreeper(Entity target, Entity mob) {
		return isCreeper(target) && target.distanceTo(mob) < CREEPER_KEEP_DISTANCE;
	}

	public static boolean isHostile(@Nullable Entity entity) {
		return entity instanceof Enemy && entity.isAlive() && !entity.isRemoved();
	}
}
