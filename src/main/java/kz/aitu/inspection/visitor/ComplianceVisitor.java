package kz.aitu.inspection.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import kz.aitu.inspection.model.*;
import static kz.aitu.inspection.visitor.Issue.Severity.CRITICAL;

public final class ComplianceVisitor extends RecursiveProjectVisitor {
    private final Set<String> prohibitedDependencies;
    private final Set<String> productionModules = new TreeSet<>();
    private final Set<String> testedModules = new TreeSet<>();
    private final List<Issue> directIssues = new ArrayList<>();

    public ComplianceVisitor() { this(Set.of("com.legacy:unsafe-crypto")); }

    public ComplianceVisitor(Set<String> prohibitedDependencies) {
        this.prohibitedDependencies = Set.copyOf(prohibitedDependencies);
    }

    @Override
    public void visit(SourceCodeFile file) {
        productionModules.add(file.module());
        credentials(file.name(), file.hardcodedCredentials());
    }

    @Override
    public void visit(TestFile file) {
        testedModules.addAll(file.coveredModules());
        credentials(file.name(), file.hardcodedCredentials());
    }

    @Override
    public void visit(ConfigurationFile file) {
        credentials(file.name(), file.exposedCredentials());
    }

    @Override
    public void visit(DependencyFile file) {
        for (Dependency dependency : file.dependencies()) {
            if (dependency.version().isBlank()) {
                directIssues.add(new Issue(CRITICAL, file.name(), "MISSING_VERSION", dependency.coordinate()));
            }
            if (prohibitedDependencies.contains(dependency.coordinate())) {
                directIssues.add(new Issue(CRITICAL, file.name(), "PROHIBITED_DEPENDENCY", dependency.coordinate()));
            }
        }
    }

    private void credentials(String name, boolean exposed) {
        if (exposed) directIssues.add(new Issue(CRITICAL, name, "EXPOSED_CREDENTIALS", "Credentials must not be exposed"));
    }

    public List<Issue> issues() {
        // Evaluate coverage after traversal, so test/source directory order does not matter.
        List<Issue> result = new ArrayList<>(directIssues);
        for (String module : productionModules) {
            if (!testedModules.contains(module)) {
                result.add(new Issue(CRITICAL, module, "UNTESTED_MODULE", "Production module has no declared tests"));
            }
        }
        return List.copyOf(result);
    }

    public boolean passed() { return issues().isEmpty(); }

    public String report() {
        List<Issue> result = issues();
        StringBuilder output = new StringBuilder("COMPLIANCE CHECK\nResult: "
                + (result.isEmpty() ? "PASS" : "FAIL") + "\nViolations: " + result.size() + "\n");
        result.forEach(issue -> output.append(issue).append('\n'));
        return output.toString();
    }
}
