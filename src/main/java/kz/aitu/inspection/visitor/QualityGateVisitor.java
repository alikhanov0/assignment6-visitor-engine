package kz.aitu.inspection.visitor;

import java.util.Locale;
import kz.aitu.inspection.model.*;

/** Composes security and statistics so the release gate uses exactly the same rules. */
public final class QualityGateVisitor implements ProjectVisitor {
    public record Result(boolean securityPass, boolean complexityPass, boolean testsPass,
                         boolean sourcePresent, boolean passed) { }

    private final ProjectStatisticsVisitor statistics = new ProjectStatisticsVisitor();
    private final SecurityAuditVisitor security = new SecurityAuditVisitor();
    private final double maxComplexity;
    private final double minTestRatio;

    public QualityGateVisitor() { this(10.0, 0.40); }

    public QualityGateVisitor(double maxComplexity, double minTestRatio) {
        if (!Double.isFinite(maxComplexity) || !Double.isFinite(minTestRatio)
                || maxComplexity < 0 || minTestRatio < 0) {
            throw new IllegalArgumentException("Quality thresholds must be finite and non-negative");
        }
        this.maxComplexity = maxComplexity;
        this.minTestRatio = minTestRatio;
    }

    @Override public void visit(SourceCodeFile file) { statistics.visit(file); security.visit(file); }
    @Override public void visit(TestFile file) { statistics.visit(file); security.visit(file); }
    @Override public void visit(ConfigurationFile file) { statistics.visit(file); security.visit(file); }
    @Override public void visit(DependencyFile file) { statistics.visit(file); security.visit(file); }
    @Override public void visit(Directory directory) {
        // Each delegate traverses the directory once. Do not also traverse here.
        statistics.visit(directory);
        security.visit(directory);
    }

    public Result result() {
        ProjectStatisticsVisitor.Statistics s = statistics.statistics();
        boolean present = s.sourceFiles() > 0;
        boolean safe = security.criticalCount() == 0;
        boolean simple = s.averageComplexity() <= maxComplexity;
        boolean tested = present && s.testSourceRatio() >= minTestRatio;
        return new Result(safe, simple, tested, present, safe && simple && tested && present);
    }

    private String status(boolean pass) { return pass ? "PASS" : "FAIL"; }

    public String report() {
        Result r = result();
        ProjectStatisticsVisitor.Statistics s = statistics.statistics();
        return String.format(Locale.ROOT,
                "QUALITY GATE%nSecurity: %s (%d critical)%nComplexity: %s (%.2f <= %.2f)%n"
                + "Tests: %s (%.2f >= %.2f)%nSource present: %s%nFINAL RESULT: %s%n",
                status(r.securityPass()), security.criticalCount(), status(r.complexityPass()),
                s.averageComplexity(), maxComplexity, status(r.testsPass()), s.testSourceRatio(),
                minTestRatio, status(r.sourcePresent()), r.passed() ? "PASSED" : "FAILED");
    }
}
