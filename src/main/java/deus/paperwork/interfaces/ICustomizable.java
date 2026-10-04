package deus.paperwork.interfaces;

import deus.paperwork.uniforms.HairPiece;
import deus.paperwork.uniforms.HairStyle;
import deus.paperwork.uniforms.UniformPiece;
import deus.paperwork.uniforms.UniformShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ICustomizable {
	@Nullable UniformPiece getUniformPiece(@NotNull UniformShape slot);
	void setUniformPiece(@NotNull UniformShape slot, @Nullable UniformPiece piece);

	@NotNull HairPiece getHairPiece();
	void setHairPiece(@NotNull HairPiece piece);
}

