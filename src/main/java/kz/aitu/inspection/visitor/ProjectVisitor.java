package kz.aitu.inspection.visitor;

import kz.aitu.inspection.model.*;

public interface ProjectVisitor {
    void visit(SourceCodeFile file);
    void visit(TestFile file);
    void visit(ConfigurationFile file);
    void visit(DependencyFile file);
    void visit(Directory directory);
}
