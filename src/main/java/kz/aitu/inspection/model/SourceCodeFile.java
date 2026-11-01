package kz.aitu.inspection.model;

import kz.aitu.inspection.visitor.ProjectVisitor;

public record SourceCodeFile(String name, String module, int linesOfCode, int complexity,
                             boolean hardcodedCredentials, boolean highRisk) implements ProjectElement {
    public SourceCodeFile {
        Validation.name(name, "name");
        Validation.name(module, "module");
        Validation.nonNegative(linesOfCode, "linesOfCode");
        Validation.nonNegative(complexity, "complexity");
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
