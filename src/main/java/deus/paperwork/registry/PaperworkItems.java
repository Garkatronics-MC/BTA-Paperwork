package deus.paperwork.registry;

import deus.paperwork.entity.cardboard_box.BoxSize;
import deus.paperwork.item.FoodAreaStick;
import deus.paperwork.item.HomeAreaStick;
import deus.paperwork.item.ItemCardboardBox;
import deus.paperwork.item.JobStick;
import deus.paperwork.item.RestAreaStick;
import deus.paperwork.item.WorkAreaStick;
import deus.paperwork.item.Stapler;
import deus.paperwork.item.base.CustomLayerItem;
import deus.paperwork.item.base.ItemPencil;
import deus.paperwork.item.base.ItemSpawner;
import deus.paperwork.item.base.PlaceableFood;
import deus.paperwork.registry.PaperworkArmorMaterials;
import deus.paperwork.entity.office_chair.EntityOfficeChair;
import deus.paperwork.entity.donut.EntityDonut;
import deus.paperwork.item.big_paperplane.ItemBigPaperPlane;
import deus.paperwork.item.letter.ItemClosedLetter;
import deus.paperwork.item.letter.ItemLetter;
import deus.paperwork.item.paperplane.ItemPaperPlane;
import deus.paperwork.registry.PaperworkToolMaterials;
import deus.utils.item.ItemFoodFactory;
import net.minecraft.core.enums.HumanArmorShape;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemArmor;
import net.minecraft.core.item.tool.ItemToolSword;
import turniplabs.halplibe.helper.ItemBuilder;

import static deus.paperwork.core.util.PaperworkConfig.itemGoc;
import static deus.paperwork.Paperwork.MOD_ID;

public class PaperworkItems {

	public static Item NEWSPRINT;
	public static Item CARDBOARD;

	public static Item ARMOR_BOOTS_CARDBOARD;
	public static Item ARMOR_LEGGINGS_CARDBOARD;
	public static Item ARMOR_CHESTPLATE_CARDBOARD;
	public static Item ARMOR_HELMET_CARDBOARD;

	public static Item PAPERPLANE;
	public static Item BIG_PAPERPLANE;

	public static Item CARDBOARD_BOX_REGULAR;
	public static Item CARDBOARD_BOX_SMALL;
	public static Item CARDBOARD_BOX_MEDIUM;
	public static Item CARDBOARD_BOX_LARGE;

	public static Item CARDBOARD_TUBE;

	public static Item LETTER;
	public static Item CLOSED_LETTER;
	public static Item STEPLER;
	public static Item IRON_NUGGET;
	public static Item PENCIL;
	public static Item FOOD_DONUT;
	public static Item FOOD_COFFE_MUG;
	public static Item OFFICE_CHAIR;

	public static Item JOB_STICK;
	public static Item WORK_AREA_STICK;
	public static Item REST_AREA_STICK;
	public static Item HOME_AREA_STICK;
	public static Item FOOD_AREA_STICK;


		public static void initialize() {

//			NEWSPRINT = new CustomLayerItem(
//				"newsprint",
//				MOD_ID + ":item/newsprint",
//				itemGoc("NEWSPRINT"),
//				(Block<? extends BlockLogicLayerBase>) PaperworkBlocks.BLOCK_NEWSPRINT_LAYER
//			);
//
//			CARDBOARD = new CustomLayerItem(
//				"cardboard",
//				MOD_ID + ":item/cardboard",
//				itemGoc("CARDBOARD"),
//				(Block<? extends BlockLogicLayerBase>) PaperworkBlocks.BLOCK_CARDBOARD_LAYER
//			);

			ARMOR_BOOTS_CARDBOARD = new ItemArmor<HumanArmorShape>(
				"armor_boots_cardboard",
				MOD_ID + ":item/armor/cardboard/boots",
				itemGoc("ARMOR_BOOTS_CARDBOARD"),
				PaperworkArmorMaterials.CARDBOARD,
				HumanArmorShape.BOOTS
			);

			ARMOR_LEGGINGS_CARDBOARD = new ItemArmor<HumanArmorShape>(
				"armor_leggings_cardboard",
				MOD_ID + ":item/armor/cardboard/leggins",
				itemGoc("ARMOR_LEGGINGS_CARDBOARD"),
				PaperworkArmorMaterials.CARDBOARD,
				HumanArmorShape.LEGS
			);

			ARMOR_CHESTPLATE_CARDBOARD = new ItemArmor<HumanArmorShape>(
				"armor_chestplate_cardboard",
				MOD_ID + ":item/armor/cardboard/chestplate",
				itemGoc("ARMOR_CHESTPLATE_CARDBOARD"),
				PaperworkArmorMaterials.CARDBOARD,
				HumanArmorShape.CHEST
			);

			ARMOR_HELMET_CARDBOARD = new ItemArmor<HumanArmorShape>(
				"armor_helmet_cardboard",
				MOD_ID + ":item/armor/cardboard/helmet",
				itemGoc("ARMOR_HELMET_CARDBOARD"),
				PaperworkArmorMaterials.CARDBOARD,
				HumanArmorShape.HEAD
			);

			PAPERPLANE = new ItemPaperPlane("paperplane", MOD_ID + ":item/paperplane/paperplane", itemGoc("PAPERPLANE"));
			BIG_PAPERPLANE = new ItemBigPaperPlane("big_paperplane", MOD_ID + ":item/big_paperplane", itemGoc("BIG_PAPERPLANE"));

			CARDBOARD_BOX_REGULAR = new ItemCardboardBox("cardboard_box_regular", MOD_ID + ":item/cardboard_box_regular", itemGoc("CARDBOARD_BOX_REGULAR"), BoxSize.REGULAR);
			CARDBOARD_BOX_SMALL = new ItemCardboardBox("cardboard_box_small", MOD_ID + ":item/cardboard_box_small", itemGoc("CARDBOARD_BOX_SMALL"), BoxSize.SMALL);
			CARDBOARD_BOX_MEDIUM = new ItemCardboardBox("cardboard_box_medium", MOD_ID + ":item/cardboard_box_medium", itemGoc("CARDBOARD_BOX_MEDIUM"), BoxSize.MEDIUM);
			CARDBOARD_BOX_LARGE = new ItemCardboardBox("cardboard_box_large", MOD_ID + ":item/cardboard_box_large", itemGoc("CARDBOARD_BOX_LARGE"), BoxSize.LARGE);

			CARDBOARD_TUBE = new ItemToolSword("cardboard_tube", MOD_ID + ":item/cardboard_tube", itemGoc("CARDBOARD_TUBE"), PaperworkToolMaterials.CARDBOARD);

			LETTER = new ItemBuilder(MOD_ID).setStackSize(1).build(new ItemLetter("letter", MOD_ID + ":item/letter/letter", itemGoc("LETTER")));
			CLOSED_LETTER = new ItemBuilder(MOD_ID).setStackSize(1).build(new ItemClosedLetter("closed_letter", MOD_ID + ":item/letter/closed_letter", itemGoc("CLOSED_LETTER")));
			STEPLER = new ItemBuilder(MOD_ID).setStackSize(1).build(new Stapler("stapler", MOD_ID + ":item/stapler", itemGoc("STEPLER")));
			IRON_NUGGET = new Item("iron_nugget", MOD_ID + ":item/iron_nugget", itemGoc("IRON_NUGGET"));
			PENCIL = new ItemPencil("pencil", MOD_ID + ":item/pencil/default", itemGoc("PENCIL"));

			FOOD_DONUT = new ItemFoodFactory<>(PlaceableFood.class, "donut", MOD_ID + ":item/donut", itemGoc("FOOD_DONUT"))
				.withHealAmount(3)
				.withTicksPerHeal(4)
				.withMaxStackSize(6)
				.build().withEntity(EntityDonut.class);

			FOOD_COFFE_MUG = new ItemFoodFactory<>("coffe_mug", MOD_ID + ":item/coffe_mug", itemGoc("FOOD_COFFE_MUG"))
				.withHealAmount(4)
				.withTicksPerHeal(3)
				.withMaxStackSize(3)
				.build();

			OFFICE_CHAIR = new ItemSpawner("office_chair",MOD_ID+":item/office_chair",itemGoc("OFFICE_CHAIR")).withEntity(
				EntityOfficeChair.class
			);

			JOB_STICK = new JobStick("job", MOD_ID+":item/job_stick", itemGoc("JOB_STICK"));
			WORK_AREA_STICK = new WorkAreaStick("work_area", MOD_ID+":item/work_area_stick", 23100);
			REST_AREA_STICK = new RestAreaStick("rest_area", MOD_ID+":item/rest_area_stick", 23101);
			HOME_AREA_STICK = new HomeAreaStick("home_area", MOD_ID+":item/home_area_stick", 23102);
			FOOD_AREA_STICK = new FoodAreaStick("food_area", MOD_ID+":item/food_area_stick", 23103);
		}


}
