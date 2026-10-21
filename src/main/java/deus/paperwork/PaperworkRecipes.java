package deus.paperwork;

import net.minecraft.core.data.registry.Registries;
import net.minecraft.core.data.registry.recipe.RecipeNamespace;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.DyeColor;
import turniplabs.halplibe.util.RecipeEntrypoint;

import java.util.ArrayList;
import java.util.List;

import static deus.paperwork.Paperwork.MOD_ID;

public class PaperworkRecipes implements RecipeEntrypoint {

	public static final RecipeNamespace PAPERWORK_RECIPE_NAMESPACE = new RecipeNamespace();

	@Override
	public void initNamespaces() {
		Registries.RECIPES.register(MOD_ID, PAPERWORK_RECIPE_NAMESPACE);

		// Registries.RECIPE_TYPES.register(MOD_ID + ":printer", RecipeEntryPrinter.class);

		List<ItemStack> dyeList = new ArrayList<>();

		DyeColor[] var17 = DyeColor.values();
		int var18 = var17.length;
		for(int var19 = 0; var19 < var18; ++var19) {
			DyeColor color = var17[var19];
			dyeList.add(new ItemStack(Items.DYE, 1, color.itemMeta));
		}
		Registries.ITEM_GROUPS.register(MOD_ID+":dyes", dyeList);

		// PAPERWORK_RECIPE_NAMESPACE.register("printer", new RecipeGroup<RecipeEntryPrinter>(new RecipeSymbol(MOD_ID+":dyes")));
	}

	@Override
	public void onRecipesReady() {
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("c c", "ccc", "ccc")
//			.addInput('c', PaperworkItems.CARDBOARD)
//			.create(MOD_ID+"armor_chestplate_cardboard", PaperworkItems.ARMOR_CHESTPLATE_CARDBOARD.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("ccc", "c c", "   ")
//			.addInput('c', PaperworkItems.CARDBOARD)
//			.create(MOD_ID+":armor_helmet_cardboard", PaperworkItems.ARMOR_HELMET_CARDBOARD.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("ccc", "c c", "c c")
//			.addInput('c', PaperworkItems.CARDBOARD)
//			.create(MOD_ID+":armor_leggings_cardboard", PaperworkItems.ARMOR_LEGGINGS_CARDBOARD.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("   ", "c c", "c c")
//			.addInput('c', PaperworkItems.CARDBOARD)
//			.create(MOD_ID+":armor_boots_cardboard", PaperworkItems.ARMOR_BOOTS_CARDBOARD.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("p  ", "   ", "   ")
//			.addInput('p', Items.PAPER)
//			.create(MOD_ID+":paperplane", PaperworkItems.PAPERPLANE.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("p p", " p ", "   ")
//			.addInput('p', Items.PAPER)
//			.create(MOD_ID+":big_paperplane", PaperworkItems.BIG_PAPERPLANE.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("ppp", "ppp", "  c")
//			.addInput('p', Items.PAPER)
//			.addInput('c', Items.TOOL_SHEARS)
//			.create(MOD_ID+":confetti_layer", new ItemStack(PaperworkBlocks.BLOCK_CONFETTI, 16));
//
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("p  ", " p ", "  p")
//			.addInput('p', PaperworkItems.CARDBOARD)
//			.create(MOD_ID+":cardboard_tube", PaperworkItems.CARDBOARD_TUBE.getDefaultStack());
//
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("pp", "pp", "   ")
//			.addInput('p', PaperworkItems.CARDBOARD)
//			.create(MOD_ID+":cardboard_box_small", PaperworkItems.CARDBOARD_BOX_SMALL.getDefaultStack());
//
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("ppp", "p p", "ppp")
//			.addInput('p', PaperworkItems.CARDBOARD)
//			.create(MOD_ID+":cardboard_box_regular", PaperworkItems.CARDBOARD_BOX_REGULAR.getDefaultStack());
//
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("ppp", "pcp", "ppp")
//			.addInput('p', PaperworkItems.CARDBOARD)
//			.addInput('c', Items.INGOT_IRON)
//			.create(MOD_ID+":cardboard_box_medium", PaperworkItems.CARDBOARD_BOX_MEDIUM.getDefaultStack());
//
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("ppp", "pcp", "ppp")
//			.addInput('p', PaperworkItems.CARDBOARD)
//			.addInput('c', Items.DIAMOND)
//			.create(MOD_ID+":cardboard_box_large", PaperworkItems.CARDBOARD_BOX_LARGE.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("ppp", "pcp", "p p")
//			.addInput('p', Items.INGOT_IRON)
//			.addInput('c', Blocks.CHEST_PLANKS_OAK)
//			.create(MOD_ID+":file_cabinet_iron", PaperworkBlocks.BLOCK_FILE_CABINET_IRON.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("ppp", "pcp", "p p")
//			.addInput('p', Blocks.PLANKS_OAK)
//			.addInput('c', Blocks.CHEST_PLANKS_OAK)
//			.create(MOD_ID+":file_cabinet_planks_oak", PaperworkBlocks.BLOCK_FILE_CABINET_BROWN_PLANKS.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("   ", "IBI", "RRR")
//			.addInput('R', Items.DUST_REDSTONE)
//			.addInput('B', Blocks.BLOCK_IRON)
//			.addInput('I', Items.INGOT_IRON)
//			.create(MOD_ID+":printer", PaperworkBlocks.BLOCK_PRINTER.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("   ", "IBI", "RRR")
//			.addInput('R', Items.DUST_REDSTONE)
//			.addInput('B', PaperworkBlocks.BLOCK_PRINTER)
//			.addInput('I', Items.INGOT_IRON)
//			.create(MOD_ID+":photocopier", PaperworkBlocks.BLOCK_PHOTOCOPIER.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("www", "www", "  f")
//			.addInput('w', Blocks.PLANKS_OAK)
//			.addInput('f', Items.FLINT)
//			.create(MOD_ID+":cardboard", new ItemStack(PaperworkItems.CARDBOARD, 64));
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("ppp", "ppp", "ddd")
//			.addInput('p', Items.PAPER)
//			.addInput('d', MOD_ID+":dyes")
//			.create(MOD_ID+":confetti", new ItemStack(PaperworkBlocks.BLOCK_CONFETTI, 16));
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("pb ", "   ", "   ")
//			.addInput('p', Items.PAPER)
//			.addInput('b', Items.PAINTBRUSH)
//			.create(MOD_ID+":letter", PaperworkItems.LETTER.getDefaultStack());
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("I  ", "   ", "   ")
//			.addInput('I', Items.INGOT_IRON)
//			.create(MOD_ID+":iron_nugget", new ItemStack(PaperworkItems.IRON_NUGGET, 9));
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("NNN", "NNN", "NNN")
//			.addInput('N', PaperworkItems.IRON_NUGGET)
//			.create(MOD_ID+":iron_nugget_to_iron", Items.INGOT_IRON.getDefaultStack());
//
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape("III", "NSS", " SS")
//			.addInput('S', Items.INGOT_STEEL)
//			.addInput('I', Items.INGOT_IRON)
//			.addInput('N', PaperworkItems.IRON_NUGGET)
//			.create(MOD_ID+":stapler", PaperworkItems.STEPLER);
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape(
//				"   ",
//				" C ",
//				"S  ")
//			.addInput('C', Items.COAL)
//			.addInput('S', Items.STICK)
//			.create(MOD_ID+":pencil", PaperworkItems.PENCIL);
//
//
//		RecipeBuilder.Shaped(MOD_ID)
//			.setShape(
//				"IWI",
//				"SWS",
//				"I I")
//			.addInput('I', Items.INGOT_IRON)
//			.addInput('S', Items.STICK)
//			.addInput('W', Blocks.WOOL)
//			.create(MOD_ID+":pencil", PaperworkItems.OFFICE_CHAIR);

//
//		RecipeGroup group = PAPERWORK_RECIPE_NAMESPACE.getItem("printer");
//		if (group == null) {
//			return;
//		}
//
//		for (Object recipe : group.getAllRecipes()) {
//			System.out.println("Receta: " + group.getKey(recipe));
//
//		}
	}
}
