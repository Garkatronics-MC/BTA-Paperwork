package deus.paperwork.gui;

import deus.paperwork.core.util.Signal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TextEditor extends Gui {

	protected Minecraft mc = Minecraft.getMinecraft();
	public int x = 0;
	public int y = 0;
	public int mx = 0;
	public int my = 0;
	public int width = 0;
	public int height = 0;

	// ? State
	private boolean wasClicked = false;
	private boolean wasClickedOut = false;
	private boolean focused = false;

	protected List<Character> characters = new ArrayList<>();
	protected final List<List<Character>> undoStack = new ArrayList<>();
	protected final List<List<Character>> redoStack = new ArrayList<>();
	public int maxUndoHistory = 100;

	protected int currentCharPos = 0;
	protected int currentLine = 1;
	protected int currentLineCharCount = 1;
	protected final List<List<Character>> clipboard = new ArrayList<>();

	// * Selection
	protected boolean isSelecting = false;
	protected int selectStartChar = 0;
	protected int selectLastChar = 0;

	// ? Drawing
	public boolean drawBackground = true;
	public boolean drawBorder = true;
	public boolean drawLineCharCount = true;
	public boolean drawLineCount = true;
	public boolean drawExtraCursors = true;

	protected int cursorX = 0;
	protected int cursorY = 0;

	// * Animation
	int cursorBlinkInterval = 500;
	private long lastCursorToggle = 0;
	public boolean drawCursor = true;
	public boolean animateCursor = true;

	String cursorCharacter = "_";
	String charCursorCharacter = ".";
	String lineCursorCharacter = "|";

	public boolean autoWrap = true;
	public int maxTextLength = 20;
	public int maxLines = 22;
	public int minTextOffsetx = 12;
	public int textOffsetX = minTextOffsetx;

	// * Colors
	public int focusBackgroundColor = 0xFF000000;
	public int focusTextColor = 0xFFE9C46A;
	public int focusBorderColor = 0xFFE9C46A;

	public int backgroundColor = 0xFF000000;
	public int textColor = 0xFFFFFFFF;
	public int borderColor = 0xFFFFFFFF;
	public int cursorColor = 0xFFFFFFFF;
	public int lineCountColor = 0xb2b3b3;

	// ? Keys
	protected boolean isCtrl = false;
	protected boolean isShift = false;

	// ? API
	public final Signal<List<Character>> $onTextChanged = new Signal<>();
	public final Signal<List<Character>> $onLostFocus = new Signal<>();

	public TextEditor() {
		currentCharPos = characters.size();
		$onTextChanged.connect(
			(s, chars)->{
				if (!undoStack.isEmpty() && undoStack.get(undoStack.size() - 1).equals(characters)) return;

				undoStack.add(new ArrayList<>(characters));
				if (undoStack.size() > maxUndoHistory) {
					undoStack.remove(0);
				}
				redoStack.clear();
			}
		);
	}

	public List<Character> cloneContent() {
		return new ArrayList<>(characters);
	}

	public void setContent(List<Character> characters) {
		this.characters = characters;
	}

	// ? Functions
	protected void paste() {
		if (!clipboard.isEmpty()) {
			$onTextChanged.emit(new ArrayList<>(this.characters));

			characters.addAll(currentCharPos, clipboard.get(clipboard.size()-1));
			currentCharPos = characters.size();

		}
	}

	public void resetCharPos() {
		currentCharPos = 0;
	}

	protected void copy(int start, int end) {
		int from = Math.min(start, end);
		int to = Math.max(start, end);

		clipboard.add(new ArrayList<>(characters.subList(from, to)));
	}

	protected void cut(int start, int end) {
		$onTextChanged.emit(new ArrayList<>(this.characters));

		copy(start, end);
		int from = Math.min(start, end);
		int to = Math.max(start, end);
		for (int i = 0; i < to - from; i++) {
			characters.remove(from);
		}

	}

	protected void undo() {
		if (!undoStack.isEmpty()) {
			redoStack.add(new ArrayList<>(characters));
			characters = undoStack.remove(undoStack.size() - 1);
			currentCharPos = Math.min(currentCharPos, characters.size());
		}

	}

	protected void redo() {
		if (!redoStack.isEmpty()) {
//			undoStack.add(new ArrayList<>(characters));
//			int index = undoStack.size() - 1;
//			if (index< redoStack.size()) {
//				characters = redoStack.remove(index);
//			}
			// currentCharPos = Math.min(currentCharPos, characters.size());
		}
	}

	protected void deleteSequence(int start, int end) {
		$onTextChanged.emit(new ArrayList<>(this.characters));

		int from = Math.min(start, end);
		int to = Math.max(start, end);
		for (int i = 0; i < to - from; i++) {
			characters.remove(from);
		}

	}

	private void deleteWord() {
		$onTextChanged.emit(new ArrayList<>(this.characters));
		while (!isAtEnd() && !isSpace(peek()) && !wordDeleteIgnore(peek())) {
			deleteCharacter();
		}
	}

	// ? Drawing functions
	protected void drawBackground() {
		int backgroundColor = focused ? focusBackgroundColor : this.backgroundColor;
		this.drawRect(this.x, this.y, this.x + width + textOffsetX, this.y + height, backgroundColor);
	}

	protected void drawBorder() {
		int borderColor = focused ? focusBorderColor : this.borderColor;

		int left = this.x;
		int right = this.x + width + textOffsetX;
		int top = this.y;
		int bottom = this.y + height;

		// Top border
		this.drawRect(left, top - 1, right, top, borderColor);

		// Bottom border
		this.drawRect(left, bottom, right, bottom + 1, borderColor);

		// Left border
		this.drawRect(left - 1, top, left, bottom, borderColor);

		// Right border
		this.drawRect(right, top, right + 1, bottom, borderColor);
	}

	protected void drawText() {
		int textColor = focused ? focusTextColor : this.textColor;
		int lineHeight = this.mc.font.getFont().fontHeight();
		int textStartY = this.y + 4;

		StringBuilder lineBuffer = new StringBuilder();
		int drawY = textStartY;

		int lineCharCount = 0;
		int cursorLine = 0;
		int pixelX = 0;

		int tempCharPos = 0;

		cursorX = 4;
		cursorY = 4;

		for (int i = 0; i < characters.size(); i++) {
			if (tempCharPos == currentCharPos) {
				cursorX = textOffsetX + pixelX;
				cursorY = 4 + (cursorLine * lineHeight);
			}

			char c = characters.get(i);

			// Evitar salto de línea automático si maxLines es 1
			if (c == '\n' || (lineCharCount >= maxTextLength && cursorLine < maxLines && maxLines > 1)) {
				this.drawStringNoShadow(this.mc.font, lineBuffer.toString(), this.x + textOffsetX, drawY, textColor);
				drawY += lineHeight;
				lineBuffer.setLength(0);
				lineCharCount = 0;
				pixelX = 0;
				cursorLine++;

				if (c != '\n') {
					lineBuffer.append(c);
					pixelX += mc.font.getFont().charWidth(c);
					lineCharCount++;
				}
			} else if (cursorLine < maxLines) {
				lineBuffer.append(c);
				pixelX +=  mc.font.getFont().charWidth(c);
				lineCharCount++;
			}

			tempCharPos++;
		}


		if (currentCharPos == characters.size()) {
			cursorX = textOffsetX + pixelX;
			cursorY = 4 + (cursorLine * lineHeight);
		}


		currentLineCharCount = lineCharCount;
		currentLine = cursorLine + 1;


		if (drawLineCount) {
			for (int i = 0; i < cursorLine + 1; i++) {
				this.drawStringNoShadow(this.mc.font, i+"", this.x, this.y + 4 + (i * mc.font.getFont().fontHeight()), lineCountColor);
			}
		}
		if (lineBuffer.length() > 0) {
			this.drawStringNoShadow(this.mc.font, lineBuffer.toString(), this.x + textOffsetX, drawY, textColor);
		}
	}

	protected void drawCursor() {
		this.drawStringNoShadow(this.mc.font, cursorCharacter, this.x + cursorX, this.y + cursorY, cursorColor);
	}

	protected void drawAlternativeCursors() {
		this.drawStringNoShadow(this.mc.font, lineCursorCharacter, this.x + width-1+textOffsetX, this.y + cursorY, 0xff0000);
		this.drawStringNoShadow(this.mc.font, charCursorCharacter, this.x + cursorX, this.y-7, 0xff0000);
	}

	protected void drawLineCharCount() {
		// String line = currentLine + "-";
		String lineCharCount = String.valueOf(currentLineCharCount);

		// this.drawString(this.mc.font, line, this.x, this.y + cursorY - mc.font.fontHeight*2, 0xb2b3b3);
		this.drawStringNoShadow(this.mc.font, lineCharCount, this.x + cursorX, this.y + cursorY + mc.font.getFont().fontHeight(), lineCountColor);
	}

	public void render() {
		if (drawBackground) drawBackground();
		if (drawBorder) drawBorder();
		if (drawLineCharCount) drawLineCharCount();
		drawText();
		if (drawCursor) drawCursor();
		if (drawExtraCursors) drawAlternativeCursors();
	}

	// ? Logic Functions
	public void updateMousePos(int mx, int my) {
		this.mx = mx;
		this.my = my;

		boolean hovered = isHovered();
		boolean buttonDown = Mouse.isButtonDown(0);

		if (hovered) {
			if (buttonDown) {
				if (!wasClicked) {
					onPush();
					wasClicked = true;
				}

				whilePressed();
				wasClickedOut = false;

			} else {
				if (wasClicked) {
					onRelease();
					wasClicked = false;
				}

				if (buttonDown) {
					if (!wasClickedOut) {
						onPushOut();
						wasClickedOut = true;
					}
				} else {
					wasClickedOut = false;
				}
			}
		} else {
			if (buttonDown) {
				if (!wasClickedOut) {
					onPushOut();
					wasClickedOut = true;
				}
			} else {
				wasClicked = false;
				wasClickedOut = false;
			}
		}
	}

	public void update() {
		if (autoWrap) {
			maxTextLength = (width/6)-1;
		}

		isCtrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);
		isShift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);

		if (drawLineCount) {
			textOffsetX = Math.max(minTextOffsetx, mc.font.stringWidth(currentLine+"")+1);
		}

		if (!focused) return;

		if (animateCursor) {
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastCursorToggle > cursorBlinkInterval) {
				drawCursor = !drawCursor;
				lastCursorToggle = currentTime;
			}
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_BACK)) {
			deleteWord();
			return;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_DELETE)) {
			deleteWord();

			return;
		}

		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				int key = Keyboard.getEventKey();
				char character = Keyboard.getEventCharacter();

				if (isShift && key == Keyboard.KEY_LEFT) {
					if (!isSelecting) {
						selectStartChar = currentCharPos;
						isSelecting = true;
					}
					if (currentCharPos > 0) {
						currentCharPos--;
					}
					selectLastChar = currentCharPos;
					return;
				}

				if (isShift && key == Keyboard.KEY_RIGHT) {
					if (!isSelecting) {
						selectStartChar = currentCharPos;
						isSelecting = true;
					}
					if (currentCharPos < characters.size()) {
						currentCharPos++;
					}
					selectLastChar = currentCharPos;
					return;
				}

				if (key == Keyboard.KEY_C && isCtrl) {
					if (selectStartChar != selectLastChar) {
						copy(selectStartChar, selectLastChar);
					}
					return;
				}
				else if (isCtrl && Keyboard.isKeyDown(Keyboard.KEY_V)) {
					paste();
					isSelecting = false;

				} else if (isCtrl && Keyboard.isKeyDown(Keyboard.KEY_Z)) {
					undo();
				} else if (isCtrl && Keyboard.isKeyDown(Keyboard.KEY_Y)) {
					redo();

				} else if (isCtrl && Keyboard.isKeyDown(Keyboard.KEY_END)) {
					currentCharPos = characters.size()-1;
				} else if (isCtrl && Keyboard.isKeyDown(Keyboard.KEY_HOME)) {
					currentCharPos = 0;

				} else if (isShift && key == Keyboard.KEY_BACK) {
					deleteSequence(selectStartChar, selectLastChar);
					return;
				}
				else if (key == Keyboard.KEY_BACK) {
					deleteCharacter();

				} else if (key == Keyboard.KEY_ESCAPE) {
					focused = false;
					$onLostFocus.emit(new ArrayList<>(this.characters));

				} else if (key == Keyboard.KEY_RETURN) {
					jumpLine();

				} else if (key == Keyboard.KEY_LEFT) {
					if (currentCharPos > 0) {
						currentCharPos--;
					}

				} else if (key == Keyboard.KEY_RIGHT) {
					if (currentCharPos < characters.size()) {
						currentCharPos++;
					}
				} else if (key == Keyboard.KEY_UP) {

				} else if (key == Keyboard.KEY_DOWN) {


				} else if (key == Keyboard.KEY_END) {
					currentCharPos = getEndOfCurrentLineCharPos();
				} else if (key == Keyboard.KEY_HOME) {
					currentCharPos = getCurrentLineCharPos();

				}  else if (Character.isDefined(character) && !Character.isISOControl(character)) {
					addCharacter(character);
				}
			}
		}
	}

	// ? Helper
	private boolean isSpace(char c) {
		return c == ' ' || c == '\n';
	}

	public static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch(NumberFormatException e){
			return false;
		}
	}

	private boolean wordDeleteIgnore(char c) {
		return isSpace(c) || c == '.' || c == ',';
	}

	private boolean isAtEnd() {
		return currentCharPos<=0;
	}

	private char peek() {
		if(currentCharPos<0)return 0;
		return characters.get(currentCharPos-1);
	}

	private char peekPrev() {
		if (characters.isEmpty()) return '\0';
		return characters.get(currentCharPos-2);
	}

	public boolean isFocused() {
		return focused;
	}

	public boolean isHovered() {
		return mx >= x && my >= y && mx < x + width && my < y + height;
	}


	// ? Utility
	private void addCharacter(char character) {
		if (currentLine <= maxLines && (maxLines > 1 || currentLineCharCount < maxTextLength)) {
			$onTextChanged.emit(new ArrayList<>(this.characters));
			characters.add(currentCharPos, character);
			currentCharPos++;
		}
	}
	private void deleteCharacter() {
		if (focused) {
			if (currentCharPos > 0) {
				$onTextChanged.emit(new ArrayList<>(this.characters));

				characters.remove(currentCharPos-1);
				currentCharPos--;

			}
		}
	}

	private void jumpLine() {
		if (currentLine-1 >= maxLines) return; // Allow jumping up to maxLines
		$onTextChanged.emit(new ArrayList<>(this.characters));
		characters.add(currentCharPos, '\n');
		currentCharPos++;
	}

	public List<String> getLines(boolean maxTextLengthSeparator) {
		return getLines(characters, maxTextLength, maxTextLengthSeparator);
	}


	public static List<String> getLines(List<Character> characters, int maxTextLength, boolean maxTextLengthSeparator) {
		List<String> lines = new ArrayList<>();
		StringBuilder lineBuffer = new StringBuilder();
		int lineCharCount = 0;
		boolean startsWithExclamation = false;

		for (char c : characters) {
			if (lineBuffer.length() == 0 && c == '!') {
				startsWithExclamation = true;
			}

			if (c == '\n') {
				lines.add(lineBuffer.toString());
				lineBuffer.setLength(0);
				lineCharCount = 0;
				startsWithExclamation = false;
			} else {
				lineBuffer.append(c);
				lineCharCount++;

				if (maxTextLengthSeparator && lineCharCount >= maxTextLength && !startsWithExclamation) {
					lines.add(lineBuffer.toString());
					lineBuffer.setLength(0);
					lineCharCount = 0;
				}
			}
		}

		if (lineBuffer.length() > 0) {
			lines.add(lineBuffer.toString());
		}

		return lines;
	}

	public String getContentAsString() {
		StringBuilder sb = new StringBuilder();
		for (Character character : characters) {
			sb.append(character);
		}
		return sb.toString();
	}

	private int getCharAt(int charX, int line) {
		return line * maxTextLength + charX;
	}

	private  int getEndOfCurrentLineCharPos() {
		int i = getCurrentLineCharPos();
		while (i < characters.size() && characters.get(i) != '\n') {
			i++;
		}
		return i;
	}

	private int getCurrentLineCharPos() {
		if (currentCharPos <= 0) return 0;
		if (currentCharPos >= characters.size()) currentCharPos = characters.size()-1;
		for (int i = currentCharPos; i >= 0; i--) {
			if (!(i == 0)&& characters.get(i-1) == '\n') {
				return i;
			}
		}
		return currentCharPos;
	}

	int charIndexInLine() {
		int startOfLine = getCurrentLineCharPos();
		return currentCharPos - startOfLine;
	}

	private int getLineNumber() {
		int line = 0;
		for (int i = 0; i < currentCharPos; i++) {
			if (characters.get(i) == '\n') {
				line++;
			}
		}
		return line;
	}


	public static String fuseStrings(List<String> strings) {
		StringBuilder sb = new StringBuilder();
		for (String character : strings) {
			sb.append(character);
		}
		return sb.toString();
	}


	private int getLineCharCount(int targetLine) {
		if (targetLine >= maxLines) return 0;

		int currentLine = 0;
		int currentLineCharCount = 0;

		for (int i = 0; i < characters.size(); i++) {
			char c = characters.get(i);

			if (c == '\n' || currentLineCharCount >= maxTextLength) {
				if (currentLine == targetLine) {
					return currentLineCharCount;
				}
				currentLine++;
				currentLineCharCount = 0;

				if (c != '\n') {
					currentLineCharCount = 1;
				}

				if (currentLine > targetLine) break;

			} else {
				currentLineCharCount++;
			}
		}

		if (currentLine == targetLine) {
			return currentLineCharCount;
		}

		return 0;
	}
	// ? API
	public void whilePressed() {
	}

	public void onPush() {
		focused = true;
	}

	public void onPushOut() {
		focused = false;
		$onLostFocus.emit(new ArrayList<>(this.characters));
	}

	public void onRelease() {
	}

}
