package deus.paperwork.core.api;

import deus.paperwork.block.printer.TileEntityPrinter;
import deus.paperwork.entity.cardboard_box.EntityCardboardBox;
import net.minecraft.core.block.entity.TileEntityFurnace;

public interface IPaperworkDisplay {
	void paperwork$displayPrinterScreen(TileEntityPrinter printer);
	void paperwork$displayCardboardBoxScreen(EntityCardboardBox cardboardBox);

}
