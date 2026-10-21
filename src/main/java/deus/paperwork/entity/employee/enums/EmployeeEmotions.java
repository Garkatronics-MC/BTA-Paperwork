package deus.paperwork.entity.employee.enums;

public enum EmployeeEmotions {
	ANGRY("paperwork:gui/icons/employee/emotions/angry"),
	HAPPY("paperwork:gui/icons/employee/emotions/happy"),
	SAD("paperwork:gui/icons/employee/emotions/sad"),
	;
	public final String path;
	EmployeeEmotions(String path) { this.path = path; }
}
