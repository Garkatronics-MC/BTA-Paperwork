package deus.paperwork.core.mixin;

import deus.paperwork.block.printer.TileEntityPrinter;
import deus.paperwork.entity.cardboard_box.EntityCardboardBox;
import deus.paperwork.gui.printer.LoginScreen;
import deus.paperwork.gui.printer.MenuPrinter;
import deus.paperwork.gui.printer.ScreenPrinter;
import deus.paperwork.core.api.IPaperworkDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PlayerLocal.class)
public class PlayerLocalMixin implements IPaperworkDisplay {

	@Override
	public void paperwork$displayPrinterScreen(TileEntityPrinter printer) {
		Minecraft mc = Minecraft.getMinecraft();
		// mc.displayScreen(new ScreenPrinter(mc.thePlayer.inventory, printer));
	}

	@Override
	public void paperwork$displayCardboardBoxScreen(EntityCardboardBox cardboardBox) {
		Minecraft mc = Minecraft.getMinecraft();
		// mc.displayScreen(new ScreenCardboardBox(mc.thePlayer.inventory, cardboardBox));
	}


}
