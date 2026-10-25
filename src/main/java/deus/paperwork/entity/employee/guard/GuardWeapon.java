package deus.paperwork.entity.employee.guard;

/// El arma que lleva equipada el guardia.
///
/// {@code NONE} — lucha a puñetazos (o con el daño base del mob).
/// {@code SWORD} — cuerpo a cuerpo, necesita estar cerca del objetivo.
/// {@code BOW} — a distancia, necesita mantenerse lejos del objetivo.
public enum GuardWeapon {
	NONE, SWORD, BOW;
}
