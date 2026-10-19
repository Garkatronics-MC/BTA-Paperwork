package deus.paperwork.uniforms;


public enum HairStyle {
	PONYTAIL    ("hair_ponytail",  "/assets/paperwork/textures/entity/employee/hairstyles/ponytail/",    1),
	HEAD        ("head_hair",      "/assets/paperwork/textures/entity/employee/hairstyles/head_hair/",   2),
	LONG        ("hair_long",      "/assets/paperwork/textures/entity/employee/hairstyles/long/",        1),
	LONG_LONG   ("hair_longlong",  "/assets/paperwork/textures/entity/employee/hairstyles/longlong/",    1),
	LONG_LONG_2 ("hair_longlong2", "/assets/paperwork/textures/entity/employee/hairstyles/longlong2/",   1),
	NONE        (null,             null,                                                                  0);

	public final String modelId;
	public final String textureBasePath;
	public final int variantCount;

	HairStyle(String modelId, String textureBasePath, int variantCount) {
		this.modelId = modelId;
		this.textureBasePath = textureBasePath;
		this.variantCount = variantCount;
	}

	public String getTexture(int variant) {
		if (textureBasePath == null) return null;
		int clamped = Math.max(0, Math.min(variant, variantCount - 1));
		return textureBasePath + clamped + ".png";
	}
}
