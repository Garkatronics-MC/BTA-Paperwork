package deus.paperwork.registry;

import net.minecraft.core.item.material.ArmorMaterial;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;

import static deus.paperwork.Paperwork.MOD_ID;

public class PaperworkArmorMaterials extends ArmorMaterial {

	public static final ArmorMaterial CARDBOARD;


	public PaperworkArmorMaterials(NamespaceID identifier, int durability) {
		super(identifier, durability);
	}

	static {
		CARDBOARD = register((new ArmorMaterial(NamespaceID.fromPool(MOD_ID, "cardboard"), 180)).withProtectionPercentage(DamageType.FALL, 20.0F).withProtectionPercentage(DamageType.FIRE, -20.0F));
	}
}
