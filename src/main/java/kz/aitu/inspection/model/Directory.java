package kz.aitu.inspection.model;

import java.util.List;
import kz.aitu.inspection.visitor.ProjectVisitor;

public record Directory(String name, List<ProjectElement> children) implements ProjectElement {
    public Directory {
        Validation.name(name, "name");
        // Defensive copy prevents later mutation and cycles through mutable child lists.
        children = List.copyOf(children);
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
