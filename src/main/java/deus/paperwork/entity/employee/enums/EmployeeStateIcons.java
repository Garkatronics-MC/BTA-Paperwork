package deus.paperwork.entity.employee.enums;

public enum EmployeeStateIcons {
	HUNGRY("paperwork:gui/icons/employee/states/hungry"),
	SCARED("paperwork:gui/icons/employee/states/scared"),
	TIRED("paperwork:gui/icons/employee/states/tired"),
	ASLEEP("paperwork:gui/icons/employee/states/asleep"),
	GOOD("paperwork:gui/icons/employee/states/good"),
	ENERGIC("paperwork:gui/icons/employee/states/energic"),
	;
	public final String path;
	EmployeeStateIcons(String path) { this.path = path; }
}
