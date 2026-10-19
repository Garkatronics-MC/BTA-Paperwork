package deus.paperwork.entities.employee.job;

/// An accessory layer of an employee: a model of its own that is drawn on top
/// of the employee, with the texture that goes with it.
///
/// Every employee shares the same base model (the body), the same uniform and
/// the same hair; the accessories are what make a farmer look like a farmer
/// (its bag, with the wheat it carries).
///
/// The model key is the one of the entity in models.json ("farmer_bag") and the
/// texture is the path of its image.
public record EmployeeAccessory(String model, String texture) {
}
