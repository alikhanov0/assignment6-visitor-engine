package kz.aitu.inspection.model;

import kz.aitu.inspection.visitor.ProjectVisitor;

public record ConfigurationFile(String name, int linesOfCode, boolean exposedCredentials,
                                boolean secureTransport, boolean debugEnabled) implements ProjectElement {
    public ConfigurationFile {
        Validation.name(name, "name");
        Validation.nonNegative(linesOfCode, "linesOfCode");
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
