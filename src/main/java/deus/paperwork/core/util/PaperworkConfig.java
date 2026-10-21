package deus.paperwork.core.util;

import deus.paperwork.Paperwork;
import deus.paperwork.registry.PaperworkBlocks;
import deus.paperwork.registry.PaperworkItems;
import net.minecraft.core.block.Block;
import net.minecraft.core.item.Item;
import turniplabs.halplibe.util.TomlConfigHandler;
import turniplabs.halplibe.util.toml.Entry;
import turniplabs.halplibe.util.toml.Toml;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PaperworkConfig {

	public static TomlConfigHandler CONFIG;
	public static final Toml TOML = new Toml("Settings :)");
	public static int START_ITEM_ID = 23000;
	public static int START_BLOCK_ID = 13000;

	public static void makeConfig() {
		configItemIDs(TOML);
		configBlockIDs(TOML);
		CONFIG = new TomlConfigHandler(Paperwork.MOD_ID, TOML);
	}


	public static void configItemIDs(Toml toml) {
		for (Field field : PaperworkItems.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) && Item.class.isAssignableFrom(field.getType())) {
				String key = "ITEM_IDs." + field.getName();
				Entry<?> id = toml.getEntry(key);
				if (id == null) {
					toml.addEntry(key, START_ITEM_ID++);
				}
			}
		}
	}


	public static void configBlockIDs(Toml toml) {
		for (Field field : PaperworkBlocks.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) && Block.class.isAssignableFrom(field.getType())) {
				String key = "BLOCK_IDs." + field.getName();
				Entry<?> id = toml.getEntry(key);
				if (id == null) {
					toml.addEntry(key, START_BLOCK_ID++);
				}
			}
		}
	}

	public static int blockGoc(String blockFieldName) {
		String key = "BLOCK_IDs." + blockFieldName;
		Integer id = CONFIG.getInt(key);
		if (id == null) {
			id = START_BLOCK_ID++;
			TOML.addEntry(key, id);
		}
		return id;
	}


	public static int itemGoc(String itemFieldName) {
		String key = "ITEM_IDs." + itemFieldName;
		Integer id = CONFIG.getInt(key);
		if (id == null) {
			id = START_ITEM_ID++;
			TOML.addEntry(key, id);
		}
		return id;
	}
}
