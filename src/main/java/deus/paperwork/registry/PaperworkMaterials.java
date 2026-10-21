package deus.paperwork.registry;

import net.minecraft.core.block.material.Material;
import net.minecraft.core.block.material.MaterialColor;
import org.jetbrains.annotations.NotNull;

public final class PaperworkMaterials  {

	public static final @NotNull Material PAPER;
	public static final @NotNull Material MONEY_PAPER;
	public static final @NotNull Material CARDBOARD;
	public static final @NotNull Material STYROFOAM;

	static {
		PAPER = new Material(MaterialColor.quartz).flammable();
		MONEY_PAPER = new Material(MaterialColor.olivine).flammable();
		CARDBOARD = new Material(MaterialColor.dirt).flammable();
		STYROFOAM = new Material(MaterialColor.quartz).flammable();
	}
}
