package kz.aitu.inspection.model;

public record Dependency(String coordinate, String version, String license, boolean vulnerable) {
    public Dependency {
        Validation.name(coordinate, "coordinate");
        // Missing version and license are allowed so visitors can report violations.
        version = version == null ? "" : version.strip();
        license = license == null ? "" : license.strip();
    }
}
