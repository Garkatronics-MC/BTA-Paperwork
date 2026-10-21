package deus.paperwork.core.api;

import deus.paperwork.core.clothing.HairPiece;
import deus.paperwork.core.clothing.UniformPiece;
import deus.paperwork.core.clothing.UniformShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ICustomizable {
	@Nullable UniformPiece getUniformPiece(@NotNull UniformShape slot);
	void setUniformPiece(@NotNull UniformShape slot, @Nullable UniformPiece piece);

	@NotNull HairPiece getHairPiece();
	void setHairPiece(@NotNull HairPiece piece);
}

