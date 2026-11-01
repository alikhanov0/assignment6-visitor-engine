package kz.aitu.inspection.model;

import java.util.List;
import kz.aitu.inspection.visitor.ProjectVisitor;

public record DependencyFile(String name, int linesOfCode, List<Dependency> dependencies) implements ProjectElement {
    public DependencyFile {
        Validation.name(name, "name");
        Validation.nonNegative(linesOfCode, "linesOfCode");
        dependencies = List.copyOf(dependencies);
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
