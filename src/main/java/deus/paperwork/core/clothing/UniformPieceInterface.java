package deus.paperwork.core.clothing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.useless.dragonfly.models.entity.StaticEntityModel;

/** Interface for uniform piece data, following the Single Responsibility Principle.
 *  Provides basic minimum information needed by the renderer, and is open for
 *  extension via new implementations without modifying existing code (Open/Closed). */
public interface UniformPieceInterface {

    /** Returns the texture reference string (e.g. "/assets/.../default.png"). */
    @NotNull String getTextureRef();

    /** Returns the model path (e.g. "geometry.paperwork.uniform.shirt"), or null if not applicable. */
    @Nullable String modelPath();

    /** Returns the base path for texture variants (e.g. "/assets/paperwork/textures/entity/employee/hairstyles/ponytail/"),
     *  or null if using a single static texture. */
    @Nullable String textureBasePath();

    /** Returns the number of texture variants (default 1 if not set). */
    int variantCount();

    /** Returns the {@link UniformShape} slot this piece belongs to. */
    UniformShape shape();

    /**
     * Returns whether this piece can overlap with other uniform pieces.
     * When true, this piece can be drawn together with other pieces (e.g. hair under a hat).
     * When false, this piece replaces/hides other pieces in the same slot (default behavior).
     * Useful for allowing hats and hair to be visible at the same time.
     */
    default boolean overlaps() {
        return false;
    }

    /**
     * Applies piece-specific uniform visibility transforms to the given model.
     * The default implementation is a no-op; subclasses can override to provide
     * shape-specific transform visibility (e.g. showing/hiding jacket/sleeve/pant transforms).
     */
    default void applyUniformVisibility(@NotNull StaticEntityModel uniform) {
        // no-op by default; renderer may fall back to switch on UniformShape
    }
}
