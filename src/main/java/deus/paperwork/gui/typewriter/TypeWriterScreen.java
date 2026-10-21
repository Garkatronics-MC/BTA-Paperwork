package deus.paperwork.gui.typewriter;

import deus.utils.define_ui.core.Widget;
import deus.utils.define_ui.integration.ReactScreen;
import deus.utils.define_ui.widgets.ContainerWidget;
import deus.utils.define_ui.widgets.textarea.TextEditorWidget;
import net.minecraft.core.player.inventory.menu.MenuAbstract;

import static deus.utils.define_ui.DefineUI.sprite;

public class TypeWriterScreen extends ReactScreen {

	private final TextEditorWidget tew = new TextEditorWidget();

	private final float editorScale = 0.60f;

	public TypeWriterScreen(MenuAbstract menu) {
		super(menu);
		this.xSize = 384;
		this.ySize = 256;

	}

	@Override
	protected Widget build() {

		ContainerWidget root = new ContainerWidget();

		Widget typewriter =
			sprite("/assets/paperwork/textures/gui/container/typewriter.png",
				0,
				0,
				384,
				256,
				384,
				256
			).build();


		tew.scale = 0.60f;

		tew.width = 384 - 32;
		tew.height = 180;

		tew.backgroundColor = 0xFFF2F2F2;
		tew.textColor = 0xFF111111;
		// tew.borderColor = 0x00000000;

		tew.focusBackgroundColor = 0xFFF2F2F2;
		tew.focusBorderColor = 0x00000000;
		tew.focusTextColor = 0xFF000000;

		tew.maxTextLength = 380;
		// tew.autoWrap = true;

		// tew.x = 76;
		// tew.y = 14;

		root.add(typewriter);
		root.add(tew);

		return root;
	}

	@Override
	public void tick() {
		super.tick();
		tew.tick();
	}

	@Override
	public void render(int mx, int my, float partialTick) {
		super.render(mx, my, partialTick);

		int centerX = (this.width - this.xSize) / 2;
		int centerY = (this.height - this.ySize) / 2;

		int absoluteTewX = centerX + tew.x;
		int absoluteTewY = centerY + tew.y;

		int scaledMouseX = (int) ((mx - absoluteTewX) / editorScale);
		int scaledMouseY = (int) ((my - absoluteTewY) / editorScale);

		tew.updateMousePos(scaledMouseX, scaledMouseY);

	}

	@Override
	public void keyPressed(char eventCharacter, int eventKey, int mx, int my) {
		if (tew.isFocused()) {
			boolean consumed = tew.onKeyTyped(eventCharacter, eventKey);
			if (consumed) {
				return; // Stops backspace from reaching Minecraft's close-screen logic
			}
		}

		super.keyPressed(eventCharacter, eventKey, mx, my);
	}
}
