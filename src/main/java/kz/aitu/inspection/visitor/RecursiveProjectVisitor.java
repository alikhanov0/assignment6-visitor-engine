package kz.aitu.inspection.visitor;

import kz.aitu.inspection.model.Directory;
import kz.aitu.inspection.model.ProjectElement;

/** Visitors own traversal. Elements perform only double dispatch. */
public abstract class RecursiveProjectVisitor implements ProjectVisitor {
    @Override
    public void visit(Directory directory) {
        for (ProjectElement child : directory.children()) {
            child.accept(this);
        }
    }
}
