package deus.paperwork.entities.employee;

import de.bsommerfeld.pathetic.api.pathing.NeighborStrategies;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicStrategies;
import de.bsommerfeld.pathetic.api.pathing.heuristic.HeuristicWeights;
import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import deus.brainless.ai.AI;
import deus.brainless.pathfinding.MobPathfinder;
import deus.paperwork.ai.AIHolder;
import deus.paperwork.ai.Brains;
import deus.paperwork.ai.pathfinding.EmployeeWalkValidator;
import deus.paperwork.entities.base.ContainerMob;
import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.enums.EmployeeEmotions;
import deus.paperwork.entities.employee.enums.EmployeeStateIcons;
import deus.paperwork.interfaces.ICustomizable;
import deus.paperwork.interfaces.WithAI;
import deus.paperwork.uniforms.HairPiece;
import deus.paperwork.uniforms.HairStyle;
import deus.paperwork.uniforms.UniformPiece;
import deus.paperwork.uniforms.UniformShape;
import deus.paperwork.util.PoscArea;
import deus.paperwork.util.RenderUtils;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntityChest;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.IItemHolding;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.player.inventory.container.ContainerSimple;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.primitives.AABBd;

import java.util.*;

@RegisterEntityRenderer(renderer = MobEmployeeRenderer.class)
@RegisterEntity(id = "employee", name = "employee")
public class MobEmployee extends MobPathfinder implements IItemHolding, ICustomizable {

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

	private static final double HUNGER_RATE = 0.0002;
	private static final double FATIGUE_RATE = 0.0001;
	private static final double WORK_RATE = 0.00015;
	private static final double SOCIAL_RATE = 0.0001;

	public Optional<TilePosc> bed_position = Optional.empty();
	public Optional<PoscArea.Area2D> food_place = Optional.empty();

	private final Map<UniformShape, UniformPiece> uniformPieces = new EnumMap<>(UniformShape.class);
	private HairPiece hairPiece = null;

	public double hunger = 0.8;
	public double fatigue = 0.1;
	public double work = 0.8;
	public double danger = 0.0;
	public double social = 0.8;
	public double health = 1.0;

	private final double traitSocial;
	private final double traitBrave;
	private final double traitHardworking;
	private final double traitLazy;


	public EmployeeAlert currentAlert = EmployeeAlert.NO_FOOD_PLACE;
	public EmployeeStateIcons currentLowState = EmployeeStateIcons.ASLEEP;
	public EmployeeEmotions currentEmotion = EmployeeEmotions.HAPPY;

	protected AIHolder<?> aiHolder;

	public final @NotNull ContainerMob inventory;


	public MobEmployee(@NotNull World world) {
		this(world, new AIHolder<>(Brains.EmployeeAI.get()));
	}

	protected MobEmployee(@NotNull World world, AIHolder<?> aiHolder) {
		super(world);
		this.setTextureIdentifier("paperwork", "employee");

		HairStyle[] styles = Arrays.stream(HairStyle.values())
			.filter(s -> s != HairStyle.NONE)
			.toArray(HairStyle[]::new);
		HairStyle randomStyle = styles[random.nextInt(styles.length)];

		hairPiece = new HairPiece(randomStyle, 0, skinToHairColor.get(this.getTextureReference()));

		uniformPieces.put(UniformShape.SHIRT, new UniformPiece("/assets/paperwork/textures/entity/employee/uniform.png"));
		uniformPieces.put(UniformShape.HAT, new UniformPiece("/assets/paperwork/textures/entity/employee/uniform.png"));
		uniformPieces.put(UniformShape.PANTS, new UniformPiece("/assets/paperwork/textures/entity/employee/uniform.png"));

		this.inventory = new ContainerMob("employee_inventory", 9);

		this.aiHolder = aiHolder;

		moveSpeed = 0.9f;

		Random rng = new Random();
		traitSocial      = 0.3 + rng.nextDouble() * 0.7;
		traitBrave       = 0.3 + rng.nextDouble() * 0.7;
		traitHardworking = 0.3 + rng.nextDouble() * 0.7;
		traitLazy        = 0.3 + rng.nextDouble() * 0.7;

		ai().update(
			input -> input
				// Initialize traits
				.set("trait_social", traitSocial)
				.set("trait_brave", traitBrave)
				.set("trait_hardworking", traitHardworking)
				.set("trait_lazy", traitLazy),
			this
		);

		configurePathfinding();
	}

	@SuppressWarnings("unchecked")
	public <T extends MobEmployee> AI<T> ai() {
		return (AI<T>) aiHolder.ai();
	}



	public boolean openChest(TilePosc posc) {
		Block<?> block = world.getBlockType(posc);
		if (!block.isEntityTile() || block.id() != Blocks.CHEST_PLANKS_OAK.id()) return false;
		TileEntityChest chest = (TileEntityChest) world.getTileEntity(posc);

		return true;
	}

	public void absorbNearbyItems(Item item) {

		AABBd box = MathHelper.aabbGrow(
			this.bb,
			3.0,
			1.0,
			3.0,
			new AABBd()
		);

		for (EntityItem entityItem :
			this.world.getEntitiesWithinAABB(EntityItem.class, box)) {

			if (!entityItem.isAlive()) {
				continue;
			}

			if (entityItem.pickupDelay > 0) {
				continue;
			}

			ItemStack stack = entityItem.item;

			if (stack == null) {
				continue;
			}

			if (!stack.getItem().equals(item)) {
				continue;
			}

			int before = stack.stackSize;

			this.inventory.insertItem(stack);

			if (stack.stackSize < before) {

				this.world.playSoundAtEntity(
					null,
					this,
					"item.pickup",
					0.2F,
					1.0F
				);

				if (stack.stackSize <= 0) {
					entityItem.remove();
				}
			}
		}
	}

	public Optional<ItemStack> findItem(TileEntityChest chest, int itemId) {
		if (chest == null) return Optional.empty();
		for (int i = 0; i < 27; i++) {
			ItemStack stack = chest.getItem(i);
			if (stack != null && stack.getItem().id == itemId) return Optional.of(stack);
		}
		return Optional.empty();
	}

	public TileEntityChest getChest(TilePosc posc) {
		Block<?> block = world.getBlockType(posc);
		if (!block.isEntityTile() || (block.id() != Blocks.CHEST_PLANKS_OAK.id() && block.id() != Blocks.CHEST_PLANKS_OAK_PAINTED.id())) return null;
		return (TileEntityChest) world.getTileEntity(posc);
	}

	public boolean eatFoodInChest(TileEntityChest chest, int itemId) {
		return findItem(chest, itemId).map(stack -> {
			eatFood(stack);
			stack.consumeItem(null);
			hunger = AI.clamp(hunger - 0.3);
			return true;
		}).orElse(false);
	}

	private void syncIcon() {
		IconCoordinate alertIcon   = TextureRegistry.getTexture(currentAlert.path);
		IconCoordinate stateIcon   = TextureRegistry.getTexture(currentLowState.path);
		IconCoordinate emotionIcon = TextureRegistry.getTexture(currentEmotion.path);
		float progressValue = (float) work;

		RenderUtils.setTarget(this, new RenderUtils.EmployeeRenderData(
			alertIcon, stateIcon, emotionIcon, progressValue
		));
	}

	private void configurePathfinding() {
		maxIterations = 10_000;
		maxLength = 128;
		strategy = NeighborStrategies.DIAGONAL_3D;
		HeuristicWeights weights = HeuristicWeights.create(
			0.0,  // Manhattan
			1.0,  // Octile — natural diagonal movement
			3.0,  // Height — still avoids climbing but less aggressive
			4.0   // Perpendicular — stronger line-following = hugs walls naturallye
		);
		pathFinderConfig = PathfinderConfiguration.builder()
			.async(true)
			.fallback(true)
			.maxIterations(maxIterations)
			.maxLength(maxLength)
			.neighborStrategy(strategy)
			.validationProcessors(buildValidators())
			.provider(provider)
			.heuristicWeights(weights)
			.heuristicStrategy(HeuristicStrategies.LINEAR)
			.build();
	}

	@Override
	protected List<ValidationProcessor> buildValidators() {
		return List.of(new EmployeeWalkValidator(this.world));

	}



	@Override
	public void tick() {
		super.tick();

		if (this.world.isClientSide) {
			return;
		}

		hunger = AI.clamp(hunger + HUNGER_RATE);
		health = (double) this.getHealth() / this.getMaxHealth();

		fatigue = AI.clamp(fatigue + FATIGUE_RATE);
		work = AI.clamp(work - WORK_RATE);
		social = AI.clamp(social - SOCIAL_RATE);


	}

	@Override
	protected void updateAI() {
//		if (work_place.isEmpty()) {
//			currentAlert = EmployeeAlert.NO_WORKPLACE;
//		} else if (food_place.isEmpty()) {
//			currentAlert = EmployeeAlert.NO_FOOD_PLACE;
//		} else if (bed_place.isEmpty()) {
//			currentAlert = EmployeeAlert.NO_REST_PLACE;
//		} else {
//			currentAlert = EmployeeAlert.GOOD;
//		}

		if (danger >= 0.51) {
			currentLowState = EmployeeStateIcons.SCARED;
		} else if (fatigue >= 0.4) {
			currentLowState = EmployeeStateIcons.TIRED;
		} else if (hunger >= 0.6) {
			currentLowState = EmployeeStateIcons.HUNGRY;
		} else {
			currentLowState = EmployeeStateIcons.GOOD;
		}

		syncIcon();

		if (this.world.isClientSide) {
			return;
		}
		double low_health = 1.0 - health;
		ai().update(
			input -> input
				.set("low_health", low_health)

				// Needs
				.set("social", social)
				.set("health", health)
				.set("danger", danger)
				.set("fatigue", fatigue)
				.set("hunger", hunger)
				.set("work", work),
			this
		);


		if (food_place.isPresent() && near(food_place.get().center(), 2.0)) {
			hunger = AI.clamp(hunger - 0.003);
		}
		if (bed_position.isPresent() && near(bed_position.get(), 2.0)) {
			fatigue = AI.clamp(fatigue - 0.004);
		}
//		if (work_place.isPresent() && near(work_place.get(), 2.5)) {
//			work = AI.clamp(work + 0.003);
//		}

		super.updateAI();
	}

	public List<TilePosc> getBlocksInArea(PoscArea.Area2D blockArea) {
		List<TilePosc> arr = new ArrayList<>();
		int min_x = Math.min(blockArea.a().x(), blockArea.b().x());
		int max_x = Math.max(blockArea.a().x(), blockArea.b().x());
		int min_z = Math.min(blockArea.a().z(), blockArea.b().z());
		int max_z = Math.max(blockArea.a().z(), blockArea.b().z());
		int y = blockArea.a().y();

		for (int i = min_x; i <= max_x; i++) {
			for (int j = min_z; j <= max_z; j++) {
				arr.add(new TilePos(i, y, j));
			}
		}

		return arr;
	}

	public List<TilePosc> getBlocksInArea(PoscArea.Area2D blockArea, int id) {
		return getBlocksInArea(blockArea).stream()
			.filter(p -> world.getBlockType(p).id() == id)
			.toList();
	}



	public boolean near(TilePosc pos, double dist) {
		double dx = pos.x() + 0.5 - this.x;
		double dz = pos.z() + 0.5 - this.z;
		return Math.sqrt(dx * dx + dz * dz) <= dist;
	}

	@Nullable
	public TilePos roamRandomPath(PoscArea.Area2D area) {

		int minX = Math.min(area.a().x(), area.b().x());
		int maxX = Math.max(area.a().x(), area.b().x());
		int minZ = Math.min(area.a().z(), area.b().z());
		int maxZ = Math.max(area.a().z(), area.b().z());

		int y = area.a().y();

		for (int i = 0; i < 10; i++) {

			int x = minX + this.random.nextInt(maxX - minX + 1);
			int z = minZ + this.random.nextInt(maxZ - minZ + 1);

			if (!this.world.isAirBlock(x, y, z)) {
				continue;
			}

			if (this.world.isAirBlock(x, y - 1, z)) {
				continue;
			}

			return new TilePos(x, y, z);
		}

		return null;
	}

	public void setDanger(double danger) { this.danger = danger; }

	@Override
	public @Nullable ItemStack getHeldItem() {
		return null;
	}

	@Override
	public void setHeldItem(@Nullable ItemStack itemStack) {

	}

	@Override
	public boolean isLeftHanded() {
		return false;
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

}
