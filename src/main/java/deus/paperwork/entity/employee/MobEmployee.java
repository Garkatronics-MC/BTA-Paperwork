package deus.paperwork.entity.employee;

import com.mojang.nbt.tags.CompoundTag;
import deus.brainless.agent.Agent;
import deus.brainless.agent.AgentMob;
import deus.brainless.goap.GoapState;
import deus.paperwork.core.ai.tasks.Food;
import deus.paperwork.entity.base.ContainerMob;
import deus.paperwork.entity.base.MobWorldAccess;
import deus.paperwork.entity.employee.enums.EmployeeAlert;
import deus.paperwork.entity.employee.enums.EmployeeEmotions;
import deus.paperwork.entity.employee.enums.EmployeeStateIcons;
import deus.paperwork.entity.employee.chef.ChefJob;
import deus.paperwork.entity.employee.guard.GuardJob;
import deus.paperwork.entity.employee.guard.GuardWeapon;
import deus.paperwork.entity.employee.job.EmployeeJob;
import deus.paperwork.entity.employee.job.EmployeeJobState;
import deus.paperwork.entity.employee.job.EmployeeJobs;
import deus.paperwork.entity.employee.model.EmployeeHudState;
import deus.paperwork.entity.employee.model.EmployeeStats;
import deus.paperwork.entity.employee.model.EmployeeStations;
import deus.paperwork.entity.employee.model.EmployeeTraits;
import deus.paperwork.core.api.ICustomizable;
import deus.paperwork.core.clothing.HairPiece;
import deus.paperwork.core.clothing.HairStyle;
import deus.paperwork.core.clothing.UniformPiece;
import deus.paperwork.core.clothing.UniformShape;
import deus.paperwork.core.util.PoscArea;
import deus.paperwork.core.util.RenderUtils;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntityChest;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.IItemHolding;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemFood;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// The employee: a job agnostic mob whose profession is a strategy.
///
/// The mob owns everything that is per mob (the job state, the inventory, the
/// stats) and delegates the profession to its {@link EmployeeJob}: the brain,
/// the stations, the recharge, the extra alert and the presentation data.
///
/// The server is the source of truth: the job, the gender, the HUD state and
/// the inventory travel to the client through the synched entity data, and the
/// client only draws them (see {@link #onLivingUpdate()}).
///
/// The dedicated server has no client classes, so the code that paints the HUD
/// lives in its own nested class ({@link ClientHud}) which the server never
/// loads.
@RegisterEntityRenderer(renderer = MobEmployeeRenderer.class)
@RegisterEntity(id = "employee", name = "employee")
public class MobEmployee extends AgentMob implements IItemHolding, ICustomizable {

	/// The hair colour of every skin of the employee, by its texture reference.
	private static final HashMap<String, Integer> skinToHairColor = new HashMap<>();

	static {
		skinToHairColor.put("0", 0xeb983f);
		skinToHairColor.put("1", 0xffc752);
		skinToHairColor.put("2", 0x24983f);
		skinToHairColor.put("3", 0xde81b0);
		skinToHairColor.put("4", 0xa95520);
		skinToHairColor.put("5", 0x2ea2ba);
		skinToHairColor.put("6", 0x36332f);
		skinToHairColor.put("7", 0x040405);
		skinToHairColor.put("8", 0x332411);
	}

	private static final int STRIKE_DAMAGE = 3;

	// how much one bite / sleep tick of recharging pays off, kept from the old passive drain
	private static final double EAT_RELIEF         = 0.3;    // one eaten item
	private static final double NEAR_FOOD_RELIEF   = 0.003;  // per tick standing at the food place
	private static final double NEAR_REST_RELIEF   = 0.004;  // per tick standing at the bed
	private static final double STATION_NEAR_RANGE = 2.0;
	private static final float  MOVE_SPEED         = 0.9f;

	// ------------------------------------------------------------------ synched data

	// 0, 1 and 2 belong to vanilla (shared flags, skin variant, health)
	private static final int DATA_JOB_ID         = 3;
	private static final int DATA_IS_WOMAN        = 4;
	private static final int DATA_ALERT           = 5;
	private static final int DATA_STATE           = 6;
	private static final int DATA_EMOTION         = 7;
	private static final int DATA_PROGRESS        = 8;
	private static final int DATA_INVENTORY_START = 9;
	private static final int DATA_HAIR_STYLE      = 18;

	/// The number of slots of the inventory of an employee.
	public static final int INVENTORY_SIZE = 9;

	// ------------------------------------------------------------------ NBT keys

	private static final String NBT_JOB       = "Job";
	private static final String NBT_WOMAN     = "Woman";
	private static final String NBT_JOB_STATE = "JobState";
	private static final String NBT_INVENTORY = "Inventory";

	/// Zone NBT keys.
	private static final String NBT_WORK_AREA = "WorkArea";
	private static final String NBT_REST_AREA = "RestArea";
	private static final String NBT_FOOD_AREA = "FoodArea";
	private static final String NBT_HOME_AREA = "HomeArea";

	/// Corner keys inside a zone tag.
	private static final String NBT_A = "A";
	private static final String NBT_B = "B";
	private static final String NBT_X = "X";
	private static final String NBT_Y = "Y";
	private static final String NBT_Z = "Z";

	private final EmployeeTraits traits = EmployeeTraits.random();
	private final EmployeeStats stats = new EmployeeStats();
	public final ContainerMob inventory = new ContainerMob("employee_inventory", INVENTORY_SIZE);

	/// The mutable state of the job of THIS employee (the fields a job needs per
	/// mob). The job itself is a shared singleton and keeps no state.
	private EmployeeJobState jobState = EmployeeJobs.plain().createState();

	/// The last inventory contents sent to the clients, to send only what changed.
	private final ItemStack[] syncedInventory = new ItemStack[INVENTORY_SIZE];

	/// The four zones assigned to this employee. A job may add its own
	/// station on top of these (e.g. the chef's workplace).
	public Optional<PoscArea.Area2D> workArea = Optional.empty();
	public Optional<PoscArea.Area2D> restArea = Optional.empty();
	public Optional<PoscArea.Area2D> foodArea = Optional.empty();
	public Optional<PoscArea.Area2D> homeArea = Optional.empty();

	/// Ticks since the last "excuse" message was sent. Used to rate-limit
	/// the chat complaints when a zone is missing something.
	private int excuseTicks = 0;

	/// How many ticks between excuse messages (600 = ~1 minute at 20 tps).
	private static final int EXCUSE_INTERVAL_TICKS = 600;

	/// Cached answer to "does the job have work right now?". The check scans
	/// the world, so it is refreshed on a timer instead of every tick.
	private boolean workAvailable = false;
	private int workAvailabilityTicks = 0;
	private static final int WORK_AVAILABILITY_INTERVAL_TICKS = 20;

	// The look of the employee: the uniform pieces it wears and its hairstyle.
	private final Map<UniformShape, UniformPiece> uniformPieces = new EnumMap<>(UniformShape.class);
	private HairPiece hairPiece = null;

	// HUD render state, mirrored from the synched data (the server publishes it)
	public EmployeeAlert currentAlert = EmployeeAlert.GOOD;
	public EmployeeStateIcons currentLowState = EmployeeStateIcons.GOOD;
	public EmployeeEmotions currentEmotion = EmployeeEmotions.HAPPY;

	public MobEmployee(@NotNull World world) {
		super(world);
		// Every employee wears the same textures now: the job adds layers on
		// top of the model (the bag of the farmer), not a texture folder.
		this.setTextureIdentifier("paperwork", "employee");
		this.moveSpeed = MOVE_SPEED;

		// The server owns the gender: it is rolled once, synced and persisted,
		// so a world reload keeps it.
		if (!this.world.isClientSide) {
			this.setWoman(this.random.nextBoolean());

			// The hairstyle is rolled by the server too, for the same reason:
			// the client has to draw the same one.
			HairStyle[] styles = Arrays.stream(HairStyle.values())
				.filter(s -> s != HairStyle.NONE)
				.toArray(HairStyle[]::new);
			this.setHairStyle(styles[random.nextInt(styles.length)]);
		}
		applyHairStyle();

		uniformPieces.put(UniformShape.SHIRT, new UniformPiece("/assets/paperwork/textures/entity/employee/uniforms/default.png"));
		uniformPieces.put(UniformShape.HAT, new UniformPiece("/assets/paperwork/textures/entity/employee/uniforms/default.png"));
		uniformPieces.put(UniformShape.PANTS, new UniformPiece("/assets/paperwork/textures/entity/employee/uniforms/default.png"));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		// This runs from the constructor of Entity, before the fields of this
		// class exist: only literals may be used here.
		this.entityData.define(DATA_JOB_ID, EmployeeJobs.PLAIN_ID, String.class);
		this.entityData.define(DATA_IS_WOMAN, (byte) 0, Byte.class);
		this.entityData.define(DATA_ALERT, (byte) EmployeeAlert.GOOD.ordinal(), Byte.class);
		this.entityData.define(DATA_STATE, (byte) EmployeeStateIcons.GOOD.ordinal(), Byte.class);
		this.entityData.define(DATA_EMOTION, (byte) EmployeeEmotions.HAPPY.ordinal(), Byte.class);
		this.entityData.define(DATA_PROGRESS, (short) 0, Short.class);
		this.entityData.define(DATA_HAIR_STYLE, (byte) HairStyle.NONE.ordinal(), Byte.class);
		for (int i = 0; i < INVENTORY_SIZE; i++) {
			this.entityData.define(DATA_INVENTORY_START + i, (ItemStack) null, ItemStack.class);
		}
	}

	// ------------------------------------------------------------------ the job

	/// The id of the job of this employee (a stable string, "plain" by default).
	/// It is synced, so the client knows which profession it is looking at.
	public String jobId() {
		return this.entityData.getString(DATA_JOB_ID);
	}

	/// The job (the strategy) of this employee.
	///
	/// @throws IllegalArgumentException when the id is not registered: an
	///         employee with a broken job must fail loudly, never behave like a
	///         plain one.
	public EmployeeJob job() {
		return EmployeeJobs.byId(jobId());
	}

	/// Gives this employee another job: the brain is rebuilt with the new
	/// strategy, the texture follows the job and a fresh job state is created.
	///
	/// The inventory and the stats are NOT touched.
	///
	/// @throws IllegalArgumentException when the id is not registered.
	public void setJob(String jobId) {
		EmployeeJob newJob = EmployeeJobs.byId(jobId);   // unknown id -> exception
		this.entityData.set(DATA_JOB_ID, newJob.id());
		AgentResetter.reset(this);                        // the brain is rebuilt
		this.jobState = newJob.createState();
		this.workAvailabilityTicks = 0;                   // re-check with the new job
		applyUniformForJob(newJob);                       // chef wears chef uniform, others default
	}

	/// Updates the uniform textures to match the job: the chef wears the chef
	/// uniform, every other job wears the default one.
	private void applyUniformForJob(EmployeeJob job) {
		String texture;
		if (job.id().equals(ChefJob.CHEF_ID)) {
			texture = "/assets/paperwork/textures/entity/employee/uniforms/chef.png";
		} else {
			texture = "/assets/paperwork/textures/entity/employee/uniforms/default.png";
		}
		UniformPiece piece = new UniformPiece(texture);
		uniformPieces.put(UniformShape.SHIRT, piece);
		uniformPieces.put(UniformShape.HAT, piece);
		uniformPieces.put(UniformShape.PANTS, piece);
	}

	/// The mutable state of the job of this employee.
	public EmployeeJobState jobState() {
		return jobState;
	}

	/// Replaces the mutable state of the job of this employee.
	public void setJobState(EmployeeJobState state) {
		if (state == null) {
			throw new IllegalArgumentException("The job state of an employee cannot be null");
		}
		this.jobState = state;
	}

	// ------------------------------------------------------------------ gender

	/// Whether this employee is a woman. Synched and persisted: the server is
	/// the source of truth and the client only mirrors it.
	public boolean isWoman() {
		return this.entityData.getByte(DATA_IS_WOMAN) != 0;
	}

	/// Sets the gender of this employee.
	public void setWoman(boolean isWoman) {
		this.entityData.set(DATA_IS_WOMAN, (byte) (isWoman ? 1 : 0));
	}

	// ------------------------------------------------------------------ the look

	/// The hairstyle of this employee. It is synced: the server rolls it once and
	/// the client draws the same one, so the employee does not change its look
	/// from one side to the other.
	public HairStyle hairStyle() {
		return hairStyleFromData(this.entityData.getByte(DATA_HAIR_STYLE));
	}

	/// Sets the hairstyle of this employee.
	public void setHairStyle(HairStyle style) {
		this.entityData.set(DATA_HAIR_STYLE, (byte) style.ordinal());
	}

	/// The hairstyle of the synced data. A value out of range means no hair.
	static HairStyle hairStyleFromData(byte raw) {
		HairStyle[] values = HairStyle.values();
		int ordinal = raw & 0xFF;
		return ordinal < values.length ? values[ordinal] : HairStyle.NONE;
	}

	/// The hairstyle whose hair piece is applied on this side.
	private HairStyle appliedHairStyle = HairStyle.NONE;

	/// Rebuilds the hair piece when the hairstyle changes (the client learns it
	/// through the synched data).
	private void applyHairStyle() {
		HairStyle style = hairStyle();
		if (hairPiece != null && style == appliedHairStyle) {
			return;
		}
		appliedHairStyle = style;
		hairPiece = new HairPiece(style, 0, skinToHairColor.get(this.getTextureReference()));
	}

	@Override
	public @Nullable UniformPiece getUniformPiece(@NotNull UniformShape slot) {
		return uniformPieces.get(slot);
	}

	@Override
	public void setUniformPiece(@NotNull UniformShape slot, @Nullable UniformPiece piece) {
		if (piece == null) uniformPieces.remove(slot);
		else uniformPieces.put(slot, piece);
	}

	@Override
	public @NotNull HairPiece getHairPiece() {
		return hairPiece;
	}

	@Override
	public void setHairPiece(@NotNull HairPiece piece) {
		this.hairPiece = piece;
	}

	// ------------------------------------------------------------------ the brain

	@Override
	protected Agent createAgent() {
		return job().buildAgent(this, traits(), stats(), stations());
	}

	@Override
	protected void sense(Agent agent) {
		// vanilla health -> [0, 1] drive input, then natural drift, then fresh inputs
		stats.syncHealth(this.getHealth(), this.getMaxHealth());
		super.sense(agent);
		stats.tick();
		stats.set(agent);
		job().sense(this, agent);
		refreshWorkAvailability();
		agent.sense("work_available", workAvailable ? 1.0 : 0.0);
	}

	/// Refreshes the cached work availability. Without work the desire to work
	/// drops, so the employee falls back to wandering instead of retrying a
	/// job that has nothing to do.
	private void refreshWorkAvailability() {
		if (workAvailabilityTicks > 0) {
			workAvailabilityTicks--;
			return;
		}
		workAvailabilityTicks = WORK_AVAILABILITY_INTERVAL_TICKS;
		workAvailable = job().hasWorkToDo(this);
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

	/// Live view of the workplace: the base stations (food, rest) plus the ones
	/// of the job. The suppliers re-read the fields every call, so stations
	/// assigned after the agent was built are picked up immediately.
	/// Unassigned stations fail their tasks fast, keeping only the built-in
	/// behaviour (wander, fight) alive.
	public EmployeeStations stations() {
		return job().stations(this, baseStations());
	}

	/// The stations every employee has: where it eats and where it rests.
	protected EmployeeStations baseStations() {
		return new EmployeeStations(
			() -> foodArea.map(area -> (PoscArea) area),
			this::foodSource,
			() -> restArea.map(area -> (PoscArea) area),
			() -> workArea.map(area -> (PoscArea) area)
		);
	}

	/// The seam the {@code eat} task bites through: any stocked chest in the
	/// food area that keeps an {@link ItemFood} stack.
	private Optional<Food> foodSource() {
		return foodArea.<Food>map(area -> new ChestFood());
	}

	/// Assigns the food zone where the employee looks for food.
	public void assignFoodArea(@Nullable PoscArea.Area2D area) {
		this.foodArea = Optional.ofNullable(area);
	}

	/// Assigns the rest zone where the employee sleeps / recovers fatigue.
	public void assignRestArea(@Nullable PoscArea.Area2D area) {
		this.restArea = Optional.ofNullable(area);
	}

	/// Assigns the work zone where the employee looks for job-specific blocks
	/// (furnace, workbench, chests, crops, ...).
	public void assignWorkArea(@Nullable PoscArea.Area2D area) {
		this.workArea = Optional.ofNullable(area);
	}

	/// Assigns the home zone. The home zone must contain a bed.
	public void assignHomeArea(@Nullable PoscArea.Area2D area) {
		this.homeArea = Optional.ofNullable(area);
	}

	/// Assigns a single bed position inside the home zone (kept for
	/// compatibility with the old employee stick).
	public void assignBed(@Nullable TilePosc bed) {
		if (bed == null) {
			this.homeArea = Optional.empty();
			this.restArea = Optional.empty();
			return;
		}
		this.homeArea = Optional.of(asArea(bed));
		this.restArea = Optional.of(asArea(bed));
	}

	/// Fires the employee: all zones and the ones of its job are cleared.
	public void clearStations() {
		workArea = Optional.empty();
		restArea = Optional.empty();
		foodArea = Optional.empty();
		homeArea = Optional.empty();
		job().clearStations(this);
	}

	private static PoscArea.Area2D asArea(TilePosc p) {
		TilePos a = new TilePos(p.x(), p.y(), p.z());
		return new PoscArea.Area2D(a, new TilePos(p.x(), p.y(), p.z()));
	}

	// ------------------------------------------------------------------ ticking

	/// The brain tick: server only. The client never runs the AI, it only
	/// draws what the server publishes (see onLivingUpdate).
	///
	/// The order matters and it is the one of the old farmer: the base
	/// recharge (food/rest), then the brain tick, and the recharge of the job
	/// AFTER the brain. The planner has to see the same work drive it saw
	/// before the refactor, so the job recharge cannot be added before the
	/// brain evaluates it.
	@Override
	protected void updateAI() {
		if (this.world.isClientSide) {
			return;
		}
		rechargeAtStations();            // the base one (food/rest)
		super.updateAI();                // the brain
		job().recharge(this, stats());   // the one of the job, AFTER the brain
		job().work(this);                // what the job does in the world
		publishHudState();
		syncInventory();
		tickExcuses();                   // complain every minute when a zone lacks something
	}

	/// Every {@link #EXCUSE_INTERVAL_TICKS} ticks, checks each assigned zone
	/// and tells the nearest player what is missing, if anything.
	private void tickExcuses() {
		excuseTicks++;
		if (excuseTicks < EXCUSE_INTERVAL_TICKS) {
			return;
		}
		excuseTicks = 0;

		String missing = missingZoneDescription();
		if (missing != null && !missing.isBlank()) {
			Player nearest = nearestPlayer();
			if (nearest != null) {
				nearest.sendMessage(TextFormatting.Base.RED,
					"[Employee] " + missing);
			}
		}
	}

	/// Describes what is missing in the assigned zones, or null when nothing
	/// is missing.
	private String missingZoneDescription() {
		StringBuilder sb = new StringBuilder();

		// Home zone must contain a bed.
		if (homeArea.isPresent() && !hasBedInArea(homeArea.get())) {
			if (sb.length() > 0) sb.append("; ");
			sb.append("no bed in home zone");
		}

		// Food zone must contain a stocked chest with food.
		if (foodArea.isPresent() && !hasFoodChestInArea(foodArea.get())) {
			if (sb.length() > 0) sb.append("; ");
			sb.append("no food in food zone");
		}

		// Rest zone should be present (even if just a bed position).
		if (restArea.isEmpty()) {
			if (sb.length() > 0) sb.append("; ");
			sb.append("no rest zone");
		}

		// Work zone: let the job decide what it needs.
		if (workArea.isPresent()) {
			String jobMissing = job().missingZoneItems(this, workArea.get());
			if (jobMissing != null && !jobMissing.isBlank()) {
				if (sb.length() > 0) sb.append("; ");
				sb.append(jobMissing);
			}
		}

		if (sb.length() > 0) {
			return sb.toString();
		}
		return null;
	}

	/// Whether the given area contains a bed block.
	private boolean hasBedInArea(PoscArea.Area2D area) {
		for (TilePosc p : PoscArea.tiles(area)) {
			if (world.getBlockType(p).id() == Blocks.BED.id()) {
				return true;
			}
		}
		return false;
	}

	/// Whether the given area contains a chest with at least one ItemFood.
	private boolean hasFoodChestInArea(PoscArea.Area2D area) {
		for (TilePosc p : PoscArea.tiles(area)) {
			TileEntityChest chest = MobWorldAccess.getChest(world, p);
			if (chest == null) continue;
			for (int i = 0; i < chest.getContainerSize(); i++) {
				ItemStack stack = chest.getItem(i);
				if (stack != null && stack.stackSize > 0
					&& stack.getItem() instanceof ItemFood) {
					return true;
				}
			}
		}
		return false;
	}

	/// The nearest player to this employee, or null when there are none.
	private Player nearestPlayer() {
		Player closest = null;
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

	/// Mirrors the old passive drain: standing at a station slowly pays the
	/// matching deficit. The one-shot bite/sleep is handled by the tasks; this
	/// is what makes the {@code rest} plan terminate off-shift. The recharge of
	/// the job is added on top of this one, after the brain tick (see updateAI).
	protected void rechargeAtStations() {
		if (foodArea.isPresent() && near(foodArea.get().center(), STATION_NEAR_RANGE)) {
			stats.eat(NEAR_FOOD_RELIEF);
		}
		if (restArea.isPresent() && near(restArea.get().center(), STATION_NEAR_RANGE)) {
			stats.rest(NEAR_REST_RELIEF);
		}
	}

	/// Same 2D distance rule the tasks use: ignores height.
	public boolean near(TilePosc pos, double dist) {
		double dx = pos.x() + 0.5 - this.x;
		double dz = pos.z() + 0.5 - this.z;
		return Math.sqrt(dx * dx + dz * dz) <= dist;
	}

	/// The presentation hook: it runs on both sides. The client rebuilds its
	/// inventory from the synced slots and paints the HUD; the server never
	/// touches the client only classes.
	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (!this.world.isClientSide) {
			return;
		}
		applyHairStyle();
		readSyncedInventory();
		paintHud();
	}

	// ------------------------------------------------------------------ HUD

	/// Computes the HUD state on the server and publishes it through the
	/// synched data. The HUD values are not persisted: they are recomputed
	/// every tick.
	private void publishHudState() {
		EmployeeHudState hud = EmployeeHudState.compute(new EmployeeHudState.Conditions(
			foodArea.isPresent(),
			restArea.isPresent(),
			job().missingStationAlert(this).orElse(null),
			stats.danger,
			stats.fatigue,
			stats.hunger,
			stats.work
		));

		this.entityData.set(DATA_ALERT, (byte) hud.alert().ordinal());
		this.entityData.set(DATA_STATE, (byte) hud.state().ordinal());
		this.entityData.set(DATA_EMOTION, (byte) hud.emotion().ordinal());
		this.entityData.set(DATA_PROGRESS, (short) Math.round(hud.progress() * EmployeeHudState.PROGRESS_SCALE));

		currentAlert = hud.alert();
		currentLowState = hud.state();
		currentEmotion = hud.emotion();
	}

	/// Reads the HUD state published by the server and paints it (client only).
	private void paintHud() {
		EmployeeHudState hud = EmployeeHudState.fromOrdinals(
			this.entityData.getByte(DATA_ALERT),
			this.entityData.getByte(DATA_STATE),
			this.entityData.getByte(DATA_EMOTION),
			this.entityData.getShort(DATA_PROGRESS)
		);

		currentAlert = hud.alert();
		currentLowState = hud.state();
		currentEmotion = hud.emotion();
		ClientHud.paint(this, hud);
	}

	// ------------------------------------------------------------------ inventory

	/// Sends the slots that changed to the clients. The server owns the
	/// inventory and it is mutated from inside the tasks (sowing eats the
	/// seeds, ...), so the diff is done against the last copy that was sent.
	private void syncInventory() {
		for (int i = 0; i < INVENTORY_SIZE; i++) {
			ItemStack stack = inventory.getItem(i);
			if (sameStack(stack, syncedInventory[i])) {
				continue;
			}
			syncedInventory[i] = stack == null ? null : stack.copy();
			this.entityData.set(DATA_INVENTORY_START + i, stack == null ? null : stack.copy());
		}
	}

	/// Rebuilds the inventory of the client from the synced slots. The client
	/// never owns the real inventory, it only mirrors it.
	private void readSyncedInventory() {
		for (int i = 0; i < INVENTORY_SIZE; i++) {
			ItemStack synced = this.entityData.getItemStack(DATA_INVENTORY_START + i);
			if (sameStack(synced, inventory.getItem(i))) {
				continue;
			}
			inventory.setItem(i, synced == null ? null : synced.copy());
		}
	}

	/// Whether two stacks hold the same thing. ItemStack has no equals(), so
	/// the item, the amount, the metadata and the custom data are compared.
	static boolean sameStack(ItemStack a, ItemStack b) {
		if (a == null || a.stackSize <= 0) return b == null || b.stackSize <= 0;
		if (b == null || b.stackSize <= 0) return false;
		if (a.itemID != b.itemID) return false;
		if (a.stackSize != b.stackSize) return false;
		if (a.getMetadata() != b.getMetadata()) return false;
		CompoundTag dataA = a.getData();
		CompoundTag dataB = b.getData();
		return dataA == null ? dataB == null : dataA.equals(dataB);
	}

	// ------------------------------------------------------------------ NBT

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString(NBT_JOB, jobId());
		tag.putBoolean(NBT_WOMAN, isWoman());

		CompoundTag jobStateTag = new CompoundTag();
		jobState().saveToNBT(jobStateTag);
		tag.putCompound(NBT_JOB_STATE, jobStateTag);

		CompoundTag inventoryTag = new CompoundTag();
		inventory.saveToNBT(inventoryTag);
		tag.putCompound(NBT_INVENTORY, inventoryTag);

		writeArea(tag, NBT_WORK_AREA, workArea);
		writeArea(tag, NBT_REST_AREA, restArea);
		writeArea(tag, NBT_FOOD_AREA, foodArea);
		writeArea(tag, NBT_HOME_AREA, homeArea);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		// The job comes first: it creates the state the saved state is loaded into.
		if (tag.containsKey(NBT_JOB)) {
			setJob(tag.getString(NBT_JOB));
		}
		if (tag.containsKey(NBT_WOMAN)) {
			setWoman(tag.getBoolean(NBT_WOMAN));
		}
		if (tag.containsKey(NBT_JOB_STATE)) {
			jobState().loadFromNBT(tag.getCompound(NBT_JOB_STATE));
		}
		if (tag.containsKey(NBT_INVENTORY)) {
			inventory.loadFromNBT(tag.getCompound(NBT_INVENTORY));
		}

		workArea = readArea(tag, NBT_WORK_AREA);
		restArea = readArea(tag, NBT_REST_AREA);
		foodArea = readArea(tag, NBT_FOOD_AREA);
		homeArea = readArea(tag, NBT_HOME_AREA);
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

	/// Tiles listed by a 2D area at its anchor height.
	public List<TilePosc> getBlocksInArea(PoscArea.Area2D blockArea) {
		return MobWorldAccess.getBlocksInArea(blockArea);
	}

	public List<TilePosc> getBlocksInArea(PoscArea.Area2D blockArea, int id) {
		return MobWorldAccess.getBlocksInArea(this.world, blockArea, id);
	}

	// ------------------------------------------------------------------ food

	/// "Where does this employee eat" made concrete: the nearest stocked chest
	/// inside the assigned food place, and one bite taken from it.
	private class ChestFood implements Food {

		@Override
		public Optional<TilePosc> nearestChest(double mobX, double mobZ) {
			TilePosc best = null;
			double bestDistance = Double.MAX_VALUE;
			PoscArea.Area2D area = foodArea.get();

			for (TilePosc p : getBlocksInArea(area)) {
				TileEntityChest chest = MobWorldAccess.getChest(world, p);
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
			TileEntityChest chest = MobWorldAccess.getChest(world, spot);
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

	/// El arma equipada si el empleado es un guardia; null en cualquier otro caso.
	/// El cliente lo usa para dibujar el ítem en la mano del empleado.
	@Override
	public @Nullable ItemStack getHeldItem() {
		GuardWeapon w = GuardJob.weaponOf(this);
		if (w == GuardWeapon.NONE) {
			return null;
		}
		int slot = GuardJob.weaponSlotOf(this);
		if (slot < 0 || slot >= inventory.getContainerSize()) {
			return null;
		}
		return inventory.getItem(slot);
	}

	/// Desea un arma al guardia: la guarda en el job state y deja el ítem en el
	/// slot indicado del inventario del empleado. El caller es responsable de
	/// quitar el ítem del inventario del jugador que se lo entrega.
	public void equipGuardWeapon(GuardWeapon weapon, int slot) {
		GuardJob.equipWeapon(this, weapon, slot);
	}

	@Override
	public void setHeldItem(@Nullable ItemStack itemStack) {
		// El ítem en la mano del guardia se gestiona a través del job state
		// (equipGuardWeapon), no directamente por esta vía.
	}

	@Override
	public boolean isLeftHanded() {
		return false;
	}

	// ------------------------------------------------------------------ client

	/// The client side of the HUD. It lives in its own class so the dedicated
	/// server never loads the client only classes (RenderUtils, TextureRegistry):
	/// only the client reaches this code.
	private static final class ClientHud {

		private ClientHud() {}

		static void paint(MobEmployee mob, EmployeeHudState hud) {
			IconCoordinate alertIcon = TextureRegistry.getTexture(hud.alert().path);
			IconCoordinate stateIcon = TextureRegistry.getTexture(hud.state().path);
			IconCoordinate emotionIcon = TextureRegistry.getTexture(hud.emotion().path);
			RenderUtils.setTarget(mob, new RenderUtils.EmployeeRenderData(
				alertIcon, stateIcon, emotionIcon, hud.progress()
			));
		}
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

	/// Writes a zone area into the tag under the given key.
	private void writeArea(CompoundTag tag, String key, Optional<PoscArea.Area2D> area) {
		area.ifPresent(a -> {
			CompoundTag zoneTag = new CompoundTag();
			writePos(zoneTag, NBT_A, a.a());
			writePos(zoneTag, NBT_B, a.b());
			tag.putCompound(key, zoneTag);
		});
	}

	/// Reads a zone area from the tag under the given key.
	private Optional<PoscArea.Area2D> readArea(CompoundTag tag, String key) {
		if (!tag.containsKey(key)) {
			return Optional.empty();
		}
		CompoundTag zoneTag = tag.getCompound(key);
		if (zoneTag == null) {
			return Optional.empty();
		}
		Optional<TilePosc> a = readPos(zoneTag, NBT_A);
		Optional<TilePosc> b = readPos(zoneTag, NBT_B);
		if (a.isEmpty() || b.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new PoscArea.Area2D(a.get(), b.get()));
	}

	private void writePos(CompoundTag tag, String key, TilePosc pos) {
		CompoundTag corner = new CompoundTag();
		corner.putInt(NBT_X, pos.x());
		corner.putInt(NBT_Y, pos.y());
		corner.putInt(NBT_Z, pos.z());
		tag.putCompound(key, corner);
	}

	private Optional<TilePosc> readPos(CompoundTag tag, String key) {
		if (!tag.containsKey(key)) {
			return Optional.empty();
		}
		CompoundTag corner = tag.getCompound(key);
		if (corner == null) {
			return Optional.empty();
		}
		return Optional.of(new TilePos(
			corner.getInteger(NBT_X),
			corner.getInteger(NBT_Y),
			corner.getInteger(NBT_Z)
		));
	}

}
