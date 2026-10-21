package deus.paperwork.core.clothing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.useless.dragonfly.models.entity.StaticEntityModel;

/** A uniform piece following the Solid Single Responsibility Principle.
 *  Holds a texture reference and optional model/texture variant data.
 *  New fields (modelPath, textureBasePath, variantCount, overlaps) are default-null/false so
 *  existing code that creates {@code new UniformPiece(ref)} continues to work. */
public record UniformPiece(
    @NotNull String textureRef,
    @Nullable String modelPath,
    @Nullable String textureBasePath,
    int variantCount,
    boolean overlaps
) implements UniformPieceInterface {

    /** Creates a uniform piece with only a texture reference; model/path/variant/overlaps default to null/false/1. */
    public UniformPiece(@NotNull String textureRef) {
        this(textureRef, null, null, 1, false);
    }

    // ---------- UniformPieceInterface implementation ----------

    @Override
    public @NotNull String getTextureRef() {
        return textureRef;
    }

    @Override
    public @Nullable String modelPath() {
        return modelPath;
    }

    @Override
    public @Nullable String textureBasePath() {
        return textureBasePath;
    }

    @Override
    public int variantCount() {
        return variantCount;
    }

    /** Returns {@link UniformShape#SHIRT} as the default slot when none is explicitly set. */
    @Override
    public UniformShape shape() {
        return UniformShape.SHIRT;
    }

    @Override
    public boolean overlaps() {
        return overlaps;
    }

    @Override
    public void applyUniformVisibility(@NotNull StaticEntityModel uniform) {
		/* Default behavior matching the old renderer switch on UniformShape:
		 * - Hide all uniform transforms first
		 * - Then show the ones matching this piece's shape */
		uniform.getTransform("jacket").visible = false;
		uniform.getTransform("rightSleeve").visible = false;
		uniform.getTransform("leftSleeve").visible = false;
		uniform.getTransform("hat").visible = false;
		uniform.getTransform("rightPant").visible = false;
		uniform.getTransform("leftPant").visible = false;

		switch (shape()) {
			case SHIRT -> {
				uniform.getTransform("jacket").visible = true;
				uniform.getTransform("rightSleeve").visible = true;
				uniform.getTransform("leftSleeve").visible = true;
			}
			case PANTS -> {
				uniform.getTransform("rightPant").visible = true;
				uniform.getTransform("leftPant").visible = true;
			}
			case HAT -> {
				uniform.getTransform("hat").visible = true;
			}
			case GLASSES -> {
				// the glasses have no geometry yet
			}
		}
    }
}