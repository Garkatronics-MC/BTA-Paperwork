package deus.paperwork.entities.employee;

import deus.brainless.agent.Agent;
import deus.brainless.agent.AgentMob;
import deus.brainless.goap.GoapState;
import deus.paperwork.ai.tasks.Food;
import deus.paperwork.entities.base.ContainerMob;
import deus.paperwork.entities.base.MobWorldAccess;
import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.enums.EmployeeEmotions;
import deus.paperwork.entities.employee.enums.EmployeeStateIcons;
import deus.paperwork.entities.employee.model.EmployeeStats;
import deus.paperwork.entities.employee.model.EmployeeStations;
import deus.paperwork.entities.employee.model.EmployeeTraits;
import deus.paperwork.util.PoscArea;
import deus.paperwork.util.RenderUtils;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.entity.TileEntityChest;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.IItemHolding;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemFood;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@RegisterEntityRenderer(renderer = MobEmployeeRenderer.class)
@RegisterEntity(id = "employee", name = "employee")
public class MobEmployee extends AgentMob implements IItemHolding {

	private static final int STRIKE_DAMAGE = 3;

	// how much one bite / sleep tick of recharging pays off, kept from the old passive drain
	private static final double EAT_RELIEF         = 0.3;    // one eaten item
	private static final double NEAR_FOOD_RELIEF   = 0.003;  // per tick standing at the food place
	private static final double NEAR_REST_RELIEF   = 0.004;  // per tick standing at the bed
	private static final double STATION_NEAR_RANGE = 2.0;
	private static final float  MOVE_SPEED         = 0.9f;

	private final EmployeeTraits traits = EmployeeTraits.random();
	private final EmployeeStats stats = new EmployeeStats();
	public final ContainerMob inventory = new ContainerMob("employee_inventory", 9);
	private final MobWorldAccess worldAccess = new MobWorldAccess(this, inventory);

	// the workplace anchors, mirroring the old Optional fields; assign via
	// assignFoodPlace / assignBed once the entity owns its places
	public Optional<PoscArea.Area2D> food_place = Optional.empty();
	public Optional<TilePosc> bed_position = Optional.empty();

	public boolean isWoman = new Random().nextInt(2) == 1;

	// HUD render state, fed to the renderer every tick (see syncIcon)
	public EmployeeAlert currentAlert = EmployeeAlert.NO_FOOD_PLACE;
	public EmployeeStateIcons currentLowState = EmployeeStateIcons.ASLEEP;
	public EmployeeEmotions currentEmotion = EmployeeEmotions.HAPPY;

	public MobEmployee(@NotNull World world) {
		super(world);
		this.setTextureIdentifier("paperwork", "employee");
		this.moveSpeed = MOVE_SPEED;
	}

	// ------------------------------------------------------------------ the brain

	@Override
	protected Agent createAgent() {
		return MobEmployeeAgent.build(this, traits, stats, stations());
	}

	@Override
	protected void sense(Agent agent) {
		// vanilla health -> [0, 1] drive input, then natural drift, then fresh inputs
		stats.syncHealth(this.getHealth(), this.getMaxHealth());
		super.sense(agent);
		stats.tick();
		stats.set(agent);
	}

	@Override
	protected Entity acquireTarget() {
		Entity closest = null;
		double closestDistance = Double.MAX_VALUE;

		for (Player player : world.players) {
			if (!player.isAlive()) continue;
			double distance = distanceTo(player);
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = player;
			}
		}
		return closest;
	}

	@Override
	protected void strike(Entity target, float distance) {
		target.hurt(this, STRIKE_DAMAGE, DamageType.COMBAT);
	}

	@Override
	protected GoapState worldState() {
		GoapState state = super.worldState();
		state.set("attacked", 0f);
		state.set("fled", 0f);
		state.set("safe", 0f);
		state.set("wandered", 0f);
		return state;
	}

	// ------------------------------------------------------------------ stations

	/// Live view of the workplace: the suppliers re-read the fields every call, so
	/// stations assigned after the agent was built are picked up immediately.
	/// Unassigned stations fail their tasks fast, keeping only the built-in
	/// behaviour (wander, fight) alive.
	public EmployeeStations stations() {
		return new EmployeeStations(
			() -> food_place.map(area -> (PoscArea) area),
			this::foodSource,
			() -> bed_position.map(MobEmployee::asArea),
			Optional::empty   // plain employees have no workplace
		);
	}

	/// The seam the {@code eat} task bites through: any stocked oak chest in the
	/// food place that keeps an {@link ItemFood} stack.
	private Optional<Food> foodSource() {
		return food_place.<Food>map(area -> new ChestFood());
	}

	public void assignFoodPlace(@Nullable PoscArea.Area2D area) {
		this.food_place = Optional.ofNullable(area);
	}

	public void assignBed(@Nullable TilePosc bed) {
		this.bed_position = Optional.ofNullable(bed);
	}

	public void clearStations() {
		food_place = Optional.empty();
		bed_position = Optional.empty();
	}

	private static PoscArea.Area2D asArea(TilePosc p) {
		TilePos a = new TilePos(p.x(), p.y(), p.z());
		return new PoscArea.Area2D(a, new TilePos(p.x(), p.y(), p.z()));
	}

	// ------------------------------------------------------------------ ticking

	@Override
	protected void updateAI() {
		// HUD state is cheap and read on both sides; the rest is server business.
		updateDisplayState();
		syncIcon();
		if (this.world.isClientSide) {
			return;
		}
		rechargeAtStations();
		super.updateAI();
	}

	/// Mirrors the old passive drain: standing at a station slowly pays the
	/// matching deficit. The one-shot bite/sleep is handled by the tasks; this
	/// is what makes the {@code rest} plan terminate off-shift. Subclasses with
	/// more stations (farmer) extend this by recharging their own deficits.
	protected void rechargeAtStations() {
		if (food_place.isPresent() && near(food_place.get().center(), STATION_NEAR_RANGE)) {
			stats.eat(NEAR_FOOD_RELIEF);
		}
		if (bed_position.isPresent() && near(bed_position.get(), STATION_NEAR_RANGE)) {
			stats.rest(NEAR_REST_RELIEF);
		}
	}

	/// Same 2D distance rule the tasks use: ignores height.
	public boolean near(TilePosc pos, double dist) {
		double dx = pos.x() + 0.5 - this.x;
		double dz = pos.z() + 0.5 - this.z;
		return Math.sqrt(dx * dx + dz * dz) <= dist;
	}

	// ------------------------------------------------------------------ HUD

	private void updateDisplayState() {
		if (danger() >= 0.51) {
			currentLowState = EmployeeStateIcons.SCARED;
		} else if (stats.fatigue >= 0.4) {
			currentLowState = EmployeeStateIcons.TIRED;
		} else if (stats.hunger >= 0.6) {
			currentLowState = EmployeeStateIcons.HUNGRY;
		} else {
			currentLowState = EmployeeStateIcons.GOOD;
		}

		if (food_place.isEmpty()) {
			currentAlert = EmployeeAlert.NO_FOOD_PLACE;
		} else if (bed_position.isEmpty()) {
			currentAlert = EmployeeAlert.NO_REST_PLACE;
		} else {
			currentAlert = EmployeeAlert.GOOD;
		}
	}

	private void syncIcon() {
		IconCoordinate alertIcon = TextureRegistry.getTexture(currentAlert.path);
		IconCoordinate stateIcon = TextureRegistry.getTexture(currentLowState.path);
		IconCoordinate emotionIcon = TextureRegistry.getTexture(currentEmotion.path);
		RenderUtils.setTarget(this, new RenderUtils.EmployeeRenderData(
			alertIcon, stateIcon, emotionIcon, (float) stats.work
		));
	}

	// ------------------------------------------------------------------ stats & helpers

	public EmployeeStats stats() {
		return stats;
	}

	/// The creator's roll; protects the immutable traits from subclasses.
	protected EmployeeTraits traits() {
		return traits;
	}

	public double danger() {
		return stats.danger;
	}

	/// External threat signal (source fired a projectile, mob is on fire, ...).
	public void setDanger(double danger) {
		stats.setDanger(danger);
	}

	/// Tiles listed by a 2D area at its anchor height (shared with the farmer).
	public List<TilePosc> getBlocksInArea(PoscArea.Area2D blockArea) {
		return worldAccess.getBlocksInArea(blockArea);
	}

	public List<TilePosc> getBlocksInArea(PoscArea.Area2D blockArea, int id) {
		return worldAccess.getBlocksInArea(blockArea, id);
	}

	// ------------------------------------------------------------------ food

	/// "Where does this employee eat" made concrete: the nearest stocked chest
	/// inside the assigned food place, and one bite taken from it.
	private class ChestFood implements Food {

		@Override
		public Optional<TilePosc> nearestChest(double mobX, double mobZ) {
			TilePosc best = null;
			double bestDistance = Double.MAX_VALUE;
			PoscArea.Area2D area = food_place.get();

			for (TilePosc p : getBlocksInArea(area)) {
				TileEntityChest chest = worldAccess.getChest(p);
				if (chest == null || !hasFood(chest)) continue;

				double dx = p.x() + 0.5 - mobX;
				double dz = p.z() + 0.5 - mobZ;
				double distance = dx * dx + dz * dz;
				if (distance < bestDistance) {
					bestDistance = distance;
					best = p;
				}
			}
			return Optional.ofNullable(best);
		}

		@Override
		public boolean eatFrom(TilePosc spot) {
			TileEntityChest chest = worldAccess.getChest(spot);
			if (chest == null) return false;

			Optional<ItemStack> found = findFood(chest);
			if (found.isEmpty()) return false;

			ItemStack stack = found.get();
			MobEmployee.this.eatFood(stack);
			stack.consumeItem(null);
			stats.eat(EAT_RELIEF);
			return true;
		}

		private boolean hasFood(TileEntityChest chest) {
			return findFood(chest).isPresent();
		}

		private Optional<ItemStack> findFood(TileEntityChest chest) {
		// 27 = a single chest's slot count, matching MobWorldAccess.findItem
			for (int i = 0; i < 27; i++) {
				ItemStack stack = chest.getItem(i);
				if (stack != null && stack.getItem() instanceof ItemFood) {
					return Optional.of(stack);
				}
			}
			return Optional.empty();
		}
	}

	// ------------------------------------------------------------------ holding

	@Override
	public @Nullable ItemStack getHeldItem() {
		return null;
	}

	@Override
	public void setHeldItem(@Nullable ItemStack itemStack) {}

	@Override
	public boolean isLeftHanded() {
		return false;
	}

	/* ---------------------------------------------------------------------
	 * TODO(brainless seam): restore the old pathfinder configuration.
	 *
	 * In Brainless v1.5.0-pre3 AgentMob's MobNavigator is private and
	 * unconfigurable, so the old employee pathfinding is commented out here.
	 *
	 * Needs in AgentMob (Brainless):
	 *     private final MobNavigator navigator;
	 *     protected AgentMob(World world) {
	 *         super(world);
	 *         this.navigator = createNavigator();
	 *     }
	 *     protected MobNavigator createNavigator() { return new MobNavigator(this); }
	 *
	 * Needs here:
	 *     @Override
	 *     protected MobNavigator createNavigator() { return new EmployeeNavigator(this); }
	 *
	 * private static class EmployeeNavigator extends MobNavigator {
	 *
	 *     EmployeeNavigator(Mob mob) {
	 *         super(mob);
	 *         maxIterations = 10_000;
	 *         maxLength = 128;
	 *         strategy = NeighborStrategies.DIAGONAL_3D;
	 *     }
	 *
	 *     @Override
	 *     protected PathfinderConfiguration configPathfinding() {
	 *         // Octile — natural diagonal movement; height damped but still
	 *         // avoids climbing; perpendicular boosted = hugs walls naturally.
	 *         HeuristicWeights weights = HeuristicWeights.create(
	 *             0.0,  // Manhattan
	 *             1.0,  // Octile
	 *             3.0,  // Height
	 *             4.0   // Perpendicular
	 *         );
	 *         return PathfinderConfiguration.builder()
	 *             .async(true)
	 *             .fallback(true)
	 *             .maxIterations(maxIterations)
	 *             .maxLength(maxLength)
	 *             .neighborStrategy(strategy)
	 *             .validationProcessors(buildValidators())
	 *             .provider(provider)
	 *             .heuristicWeights(weights)
	 *             .heuristicStrategy(HeuristicStrategies.LINEAR)
	 *             .build();
	 *     }
	 *
	 *     @Override
	 *     protected List<ValidationProcessor> buildValidators() {
	 *         return List.of(new EmployeeWalkValidator(mob.world));
	 *     }
	 * }
	 * ------------------------------------------------------------------ */
}
