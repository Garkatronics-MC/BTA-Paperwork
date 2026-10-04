package deus.paperwork.uniforms;

import org.jetbrains.annotations.NotNull;

public record HairPiece(
	@NotNull HairStyle style,
	int variant,
	int rgb
) {
	public String resolveTexture() {
		return style.getTexture(variant);
	}

	public static final int NO_TINT = -1;

	public boolean hasTint() {
		return rgb != NO_TINT;
	}
}
