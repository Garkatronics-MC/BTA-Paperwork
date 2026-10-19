package deus.paperwork.entities.base;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import com.mojang.nbt.tags.Tag;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.player.inventory.container.ContainerSimple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContainerMob implements Container {

	/// Key under which the slots of the container are stored inside the NBT.
	/// The value is a list with one compound per slot, in slot order.
	public static final String SLOTS_KEY = "Slots";

	/// Key used inside every slot compound to hold the NBT of the item of that slot.
	/// A slot without this key is an empty slot.
	public static final String ITEM_KEY = "Item";

	private final ContainerSimple container;

	public ContainerMob(@NotNull String name, int size) {
		this.container = new ContainerSimple(name, size);
	}

	public int insertItem(@NotNull ItemStack stackToAdd) {
		return insertInto(container, stackToAdd);
	}

	private int insertInto(ContainerSimple inv, ItemStack stackToAdd) {
		int original = stackToAdd.stackSize;

		for (int i = 0; i < inv.getContainerSize(); i++) {

			ItemStack stackInSlot = inv.getItem(i);

			if (stackInSlot != null && stackInSlot.canStackWith(stackToAdd)) {

				int transfer = Math.min(
					stackToAdd.stackSize,
					stackInSlot.getMaxStackSize() - stackInSlot.stackSize
				);

				transfer = Math.min(
					transfer,
					stackToAdd.getMaxStackSize()
				);

				if (transfer <= 0) continue;

				stackInSlot.stackSize += transfer;
				stackToAdd.stackSize -= transfer;

				if (stackToAdd.stackSize <= 0) {
					return original;
				}
			}
		}

		for (int i = 0; i < inv.getContainerSize(); i++) {

			if (inv.getItem(i) == null) {

				int transfer = Math.min(
					stackToAdd.stackSize,
					stackToAdd.getMaxStackSize()
				);

				ItemStack copy = stackToAdd.copy();
				copy.stackSize = transfer;

				inv.setItem(i, copy);

				stackToAdd.stackSize -= transfer;

				if (stackToAdd.stackSize <= 0) {
					return original;
				}
			}
		}

		return original - stackToAdd.stackSize;
	}

	public boolean available(Item item) {
		if (item == null) {
			return false;
		}

		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);

			if (stack != null
				&& stack.itemID == item.id
				&& stack.stackSize > 0) {
				return true;
			}
		}

		return false;
	}

	public @Nullable ItemStack findStackOf(@NotNull Item item) {
		if (item == null) {
			return null;
		}

		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);

			if (stack != null
				&& stack.itemID == item.id
				&& stack.stackSize > 0) {
				return getItem(i);
			}
		}

		return null;
	}

	@Override
	public int getContainerSize() {
		return container.getContainerSize();
	}

	@Override
	public @Nullable ItemStack getItem(int i) {
		return container.getItem(i);
	}

	@Override
	public @Nullable ItemStack removeItem(int i, int i1) {
		return container.removeItem(i, i1);
	}

	@Override
	public void setItem(int i, @Nullable ItemStack itemStack) {
		container.setItem(i, itemStack);
	}

	@Override
	public @NotNull String getNameTranslationKey() {
		return container.getNameTranslationKey();
	}

	@Override
	public int getMaxStackSize() {
		return container.getMaxStackSize();
	}

	@Override
	public void setChanged() {
		container.setChanged();
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return container.stillValid(player);
	}

	@Override
	public void sort() {
		container.sort();
	}

	@Override
	public boolean locked(int slot) {
		return container.locked(slot);
	}

	/// Writes the whole content of the container into the given tag.
	///
	/// Layout: `tag["Slots"]` is a list with one compound per slot, in slot order.
	/// An empty slot is written as an empty compound (no "Item" key inside), never as
	/// a stack of zero. A filled slot is written as `{ "Item": <ItemStack NBT> }`, so
	/// the item id, the amount and the item NBT (custom data, ...) are all kept.
	///
	/// Saving twice with the same content produces the same tag, and
	/// `save` -> `load` -> `save` is stable.
	public void saveToNBT(@NotNull CompoundTag tag) {
		ListTag slots = new ListTag();

		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			CompoundTag slot = new CompoundTag();

			if (stack != null && stack.stackSize > 0) {
				CompoundTag item = new CompoundTag();
				stack.writeToNBT(item);
				slot.putCompound(ITEM_KEY, item);
			}

			slots.addTag(slot);
		}

		tag.putList(SLOTS_KEY, slots);
	}

	/// Reads the slots from the given tag and REPLACES the current content of the
	/// container: slots that are not in the tag end up empty.
	///
	/// A missing tag, a tag without "Slots" or an empty slot list are all valid and
	/// simply leave the container empty.
	public void loadFromNBT(@Nullable CompoundTag tag) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			container.setItem(i, null);
		}

		if (tag == null) {
			return;
		}

		ListTag slots = tag.getList(SLOTS_KEY);
		int size = container.getContainerSize();

		for (int i = 0; i < slots.tagCount() && i < size; i++) {
			Tag<?> entry = slots.tagAt(i);

			if (!(entry instanceof CompoundTag slot) || !slot.containsKey(ITEM_KEY)) {
				continue;
			}

			ItemStack stack = ItemStack.readItemStackFromNbt(slot.getCompound(ITEM_KEY));

			if (stack != null) {
				container.setItem(i, stack);
			}
		}
	}
}
