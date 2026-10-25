package deus.paperwork.entity.employee.guard;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.entity.employee.job.EmployeeJobState;

/// El estado mutable del oficio guard: qué arma lleva equipada y en qué slot
/// del inventario del empleado la lleva.
///
/// Todo lo mutable vive aquí (el job es un singleton sin estado) y se guarda
/// en el NBT del mob, así que el guardia conserva su arma tras recargar el
/// mundo.
public class GuardJobState extends EmployeeJobState {

	public static final String NBT_WEAPON   = "Weapon";
	public static final String NBT_WEAPON_SLOT = "WeaponSlot";

	/// El arma equipada. {@link GuardWeapon#NONE} cuando no lleva nada.
	private GuardWeapon weapon = GuardWeapon.NONE;

	/// Slot del inventario del empleado donde está el arma. {@code -1} si no hay.
	private int weaponSlot = -1;

	public GuardWeapon weapon() {
		return weapon;
	}

	public int weaponSlot() {
		return weaponSlot;
	}

	/// Equipa un arma. Un slot de {@code -1} limpia el equipado.
	public void setWeapon(GuardWeapon weapon, int weaponSlot) {
		this.weapon = weapon;
		this.weaponSlot = weaponSlot;
	}

	/// Deja de llevar arma.
	public void clearWeapon() {
		this.weapon = GuardWeapon.NONE;
		this.weaponSlot = -1;
	}

	// ------------------------------------------------------------------ NBT

	@Override
	public void saveToNBT(CompoundTag tag) {
		tag.putString(NBT_WEAPON, weapon.name());
		tag.putInt(NBT_WEAPON_SLOT, weaponSlot);
	}

	@Override
	public void loadFromNBT(CompoundTag tag) {
		if (tag == null) {
			clearWeapon();
			return;
		}
		try {
			weapon = GuardWeapon.valueOf(tag.getString(NBT_WEAPON));
		} catch (IllegalArgumentException e) {
			weapon = GuardWeapon.NONE;
		}
		weaponSlot = tag.getInteger(NBT_WEAPON_SLOT);
		if (weaponSlot < 0) {
			weaponSlot = -1;
			weapon = GuardWeapon.NONE;
		}
	}
}
