package deus.paperwork.entities.base;

import net.minecraft.core.Global;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.SkinVariantList;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TexturedEntity extends Entity {

	protected @NotNull NamespaceID textureIdentifier = NamespaceID.fromPool("minecraft", "char");

	public TexturedEntity(@Nullable World world) {
		super(world);
		int variant = this.random.nextInt(4);
		this.entityData.define(1, (byte)variant, Byte.class);
		this.setSkinVariant(variant);
	}

	public String getEntityTexture() {
		String basePath = String.format("/assets/%s/textures/entity/%s/", this.textureIdentifier.namespace(), this.textureIdentifier.value());
		return basePath + this.getTextureReference() + "1.png";
	}

	public String getTextureReference() {
		SkinVariantList variantList = Global.accessor.getSkinVariantList();
		String basePath = String.format("/assets/%s/textures/entity/%s/", this.textureIdentifier.namespace(), this.textureIdentifier.value());
		return variantList.getSkinReference(basePath + "variants.json", "0", this.getSkinVariant());
	}
	public int getSkinVariant() {
		try {
			return Byte.toUnsignedInt(this.entityData.getByte(1));
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public boolean cycleVariant() {
		String basePath = String.format("/assets/%s/textures/entity/%s/", this.textureIdentifier.namespace(), this.textureIdentifier.value());
		int skinVar = this.getSkinVariant();
		this.setSkinVariant(Global.accessor.getSkinVariantList().nextSkinVariant(basePath + "variants.json", skinVar));
		return skinVar != this.getSkinVariant();
	}

	public void setSkinVariant(int skinVariant) {
		this.entityData.set(1, (byte)skinVariant);
	}

	public @NotNull String getDefaultEntityTexture() {
		return String.format("/assets/%s/textures/entity/%s/0.png", this.textureIdentifier.namespace(), this.textureIdentifier.value());
	}


}
