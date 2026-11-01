package kz.aitu.inspection.visitor;

import java.util.ArrayList;
import java.util.List;
import kz.aitu.inspection.model.*;
import static kz.aitu.inspection.visitor.Issue.Severity.*;

public final class SecurityAuditVisitor extends RecursiveProjectVisitor {
    private final List<Issue> issues = new ArrayList<>();

    @Override
    public void visit(SourceCodeFile file) {
        credentials(file.name(), file.hardcodedCredentials());
        if (file.highRisk()) {
            issues.add(new Issue(CRITICAL, file.name(), "HIGH_RISK_SOURCE", "High-risk source requires review"));
        }
    }

    @Override
    public void visit(TestFile file) {
        credentials(file.name(), file.hardcodedCredentials());
    }

    @Override
    public void visit(ConfigurationFile file) {
        credentials(file.name(), file.exposedCredentials());
        if (!file.secureTransport()) {
            issues.add(new Issue(CRITICAL, file.name(), "INSECURE_TRANSPORT", "Enable secure transport"));
        }
        if (file.debugEnabled()) {
            issues.add(new Issue(WARNING, file.name(), "DEBUG_ENABLED", "Disable production debug mode"));
        }
    }

    @Override
    public void visit(DependencyFile file) {
        for (Dependency dependency : file.dependencies()) {
            if (dependency.vulnerable()) {
                issues.add(new Issue(CRITICAL, file.name(), "VULNERABLE_DEPENDENCY",
                        dependency.coordinate() + ":" + dependency.version() + " is flagged vulnerable"));
            }
        }
    }

    private void credentials(String name, boolean exposed) {
        if (exposed) {
            issues.add(new Issue(CRITICAL, name, "EXPOSED_CREDENTIALS", "Remove exposed credentials"));
        }
    }

    public List<Issue> issues() { return List.copyOf(issues); }
    public long criticalCount() { return issues.stream().filter(i -> i.severity() == CRITICAL).count(); }
    public long warningCount() { return issues.stream().filter(i -> i.severity() == WARNING).count(); }
    public int score() { return (int) Math.max(0L, 100L - 20L * criticalCount() - 5L * warningCount()); }

    public String report() {
        StringBuilder output = new StringBuilder("SECURITY AUDIT\nScore: " + score() + "/100\nCritical: "
                + criticalCount() + ", warnings: " + warningCount() + "\n");
        if (issues.isEmpty()) output.append("No security issues\n");
        issues.forEach(issue -> output.append(issue).append('\n'));
        return output.toString();
    }
}
