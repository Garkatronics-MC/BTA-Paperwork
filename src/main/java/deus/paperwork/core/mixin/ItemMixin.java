package deus.paperwork.core.mixin;

import deus.paperwork.core.api.IItemWeight;
import net.minecraft.core.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Item.class)
public class ItemMixin implements IItemWeight {

	@Unique double weight = 0.0001F;

	@Override
	public double paperwork$getWeight() {
		return weight;
	}

	@Override
	public void paperwork$setWeight(double weight) {
		this.weight = weight;
	}
}
