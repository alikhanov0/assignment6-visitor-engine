package kz.aitu.inspection.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kz.aitu.inspection.model.*;
import static kz.aitu.inspection.visitor.Issue.Severity.*;

/** Additional operation: no changes to any existing element or visitor interface. */
public final class LicenseAuditVisitor extends RecursiveProjectVisitor {
    private final Set<String> approvedLicenses;
    private final List<Issue> issues = new ArrayList<>();
    private long checkedDependencies;

    public LicenseAuditVisitor() {
        this(Set.of("MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC"));
    }

    public LicenseAuditVisitor(Set<String> approvedLicenses) {
        this.approvedLicenses = Set.copyOf(approvedLicenses);
    }

    @Override public void visit(SourceCodeFile file) { }
    @Override public void visit(TestFile file) { }
    @Override public void visit(ConfigurationFile file) { }

    @Override
    public void visit(DependencyFile file) {
        for (Dependency dependency : file.dependencies()) {
            checkedDependencies++;
            if (dependency.license().isBlank()) {
                issues.add(new Issue(WARNING, file.name(), "UNKNOWN_LICENSE", dependency.coordinate()));
            } else if (!approvedLicenses.contains(dependency.license())) {
                issues.add(new Issue(CRITICAL, file.name(), "UNAPPROVED_LICENSE",
                        dependency.coordinate() + " uses " + dependency.license()));
            }
        }
    }

    public long checkedDependencies() { return checkedDependencies; }
    public List<Issue> issues() { return List.copyOf(issues); }
    public boolean passed() { return issues.isEmpty(); }

    public String report() {
        StringBuilder output = new StringBuilder("LICENSE AUDIT\nDependencies checked: " + checkedDependencies
                + "\nResult: " + (passed() ? "PASS" : "FAIL") + "\n");
        issues.forEach(issue -> output.append(issue).append('\n'));
        return output.toString();
    }
}
