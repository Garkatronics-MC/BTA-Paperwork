package deus.paperwork.core.mixin;

import deus.paperwork.block.printer.TileEntityPrinter;
import deus.paperwork.entity.cardboard_box.EntityCardboardBox;
import deus.paperwork.gui.printer.LoginScreen;
import deus.paperwork.gui.printer.MenuPrinter;
import deus.paperwork.gui.printer.ScreenPrinter;
import deus.paperwork.gui.typewriter.TypeWriterScreen;
import deus.paperwork.core.api.IPaperworkDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.block.entity.TileEntityFurnace;
import net.minecraft.core.player.inventory.menu.MenuFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	@Inject(method = "displayFurnaceScreen", at = @At("HEAD"), cancellable = true)
	private void onDisplayFurnace(TileEntityFurnace furnace, CallbackInfo ci) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.displayScreen(new TypeWriterScreen(new MenuPrinter(mc.thePlayer.inventory, null)));
		ci.cancel();
	}


}
