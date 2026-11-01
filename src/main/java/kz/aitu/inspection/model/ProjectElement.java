package kz.aitu.inspection.model;

import kz.aitu.inspection.visitor.ProjectVisitor;

public interface ProjectElement {
    void accept(ProjectVisitor visitor);
}
