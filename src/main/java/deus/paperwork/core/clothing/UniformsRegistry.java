package deus.paperwork.core.clothing;

/** Registry of uniform piece definitions, centralizing creation and
 *  providing a single point of reference for all uniform pieces.
 *  Follows the Single Responsibility Principle: this class only knows about
 *  uniform piece definitions, not about employees or rendering. */
public final class UniformsRegistry {

	private UniformsRegistry() { }

	public static final UniformPiece SHIRT = new UniformPiece(
		"/assets/paperwork/textures/entity/employee/uniforms/default.png",
		"geometry.paperwork.uniform.shirt",
		null,
		1,
		false
	);

	public static final UniformPiece PANTS = new UniformPiece(
		"/assets/paperwork/textures/entity/employee/uniforms/default.png",
		"geometry.paperwork.uniform.pants",
		null,
		1,
		false
	);

	public static final UniformPiece HAT = new UniformPiece(
		"/assets/paperwork/textures/entity/employee/uniforms/default.png",
		"geometry.paperwork.uniform.hat",
		null,
		1,
		true
	);

	public static final UniformPiece GLASSES = new UniformPiece(
		"/assets/paperwork/textures/entity/employee/uniforms/default.png",
		null,
		null,
		1,
		false
	);

	/** Hair uniform piece: employee hair.
	 *  Can overlap with other pieces (e.g. hats), allowing both hair and hat
	 *  to be visible at the same time. Uses texture base path for variants. */
	public static final UniformPiece HAIR = new UniformPiece(
		"/assets/paperwork/textures/entity/employee/hair.png",
		"geometry.paperwork.hair",
		"/assets/paperwork/textures/entity/employee/hairstyles/",
		2,
		true
	);
}
