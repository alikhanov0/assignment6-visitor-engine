package kz.aitu.inspection.model;

import java.util.Set;
import kz.aitu.inspection.visitor.ProjectVisitor;

public record TestFile(String name, int linesOfCode, int testCases,
                       Set<String> coveredModules, boolean hardcodedCredentials) implements ProjectElement {
    public TestFile {
        Validation.name(name, "name");
        Validation.nonNegative(linesOfCode, "linesOfCode");
        Validation.nonNegative(testCases, "testCases");
        coveredModules = Set.copyOf(coveredModules);
        coveredModules.forEach(module -> Validation.name(module, "covered module"));
        if (testCases == 0 && !coveredModules.isEmpty()) {
            throw new IllegalArgumentException("A test file with no test cases cannot cover modules");
        }
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
