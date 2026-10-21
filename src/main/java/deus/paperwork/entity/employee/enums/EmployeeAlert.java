package deus.paperwork.entity.employee.enums;

public enum EmployeeAlert {
	GENERIC("paperwork:gui/icons/employee/alerts/generic"),
	MONSTER("paperwork:gui/icons/employee/alerts/monster"),
	NO_WORKPLACE("paperwork:gui/icons/employee/alerts/no_workplace"),
	NO_FOOD_PLACE("paperwork:gui/icons/employee/alerts/no_food_place"),
	NO_REST_PLACE("paperwork:gui/icons/employee/alerts/no_rest_place"),
	GREETING_BOSS("paperwork:gui/icons/employee/alerts/greeting_boss"),
	GREETING("paperwork:gui/icons/employee/alerts/greeting"),
	GOOD("paperwork:gui/icons/employee/alerts/good"),
	;
	public final String path;
	EmployeeAlert(String path) { this.path = path; }
}
