package kz.aitu.inspection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import kz.aitu.inspection.model.*;
import kz.aitu.inspection.visitor.*;
import static org.junit.jupiter.api.Assertions.*;

class InspectionTest {
    private SourceCodeFile source(String module, int complexity) {
        return new SourceCodeFile(module + ".java", module, 100, complexity, false, false);
    }

    private TestFile testFile(String module) {
        return new TestFile(module + "Test.java", 50, 3, Set.of(module), false);
    }

    private Directory root(ProjectElement... elements) { return new Directory("root", List.of(elements)); }

    @Test
    void dispatchesEveryElementTypeAndNestedDirectoriesInOrder() {
        List<String> visits = new ArrayList<>();
        ProjectVisitor visitor = new ProjectVisitor() {
            public void visit(SourceCodeFile f) { visits.add("source:" + f.name()); }
            public void visit(TestFile f) { visits.add("test:" + f.name()); }
            public void visit(ConfigurationFile f) { visits.add("config:" + f.name()); }
            public void visit(DependencyFile f) { visits.add("deps:" + f.name()); }
            public void visit(Directory d) {
                visits.add("dir:" + d.name());
                d.children().forEach(e -> e.accept(this));
            }
        };
        root(new Directory("nested", List.of(source("payments", 5))), testFile("payments"),
                new ConfigurationFile("app", 2, false, true, false),
                new DependencyFile("pom", 3, List.of())).accept(visitor);
        assertEquals(List.of("dir:root", "dir:nested", "source:payments.java", "test:paymentsTest.java",
                "config:app", "deps:pom"), visits);
    }

    @Test
    void computesAllStatisticsForFintechProject() {
        ProjectStatisticsVisitor visitor = new ProjectStatisticsVisitor();
        DemoProjects.fintechProject().accept(visitor);
        assertEquals(new ProjectStatisticsVisitor.Statistics(5, 4, 2, 1, 8, 530, 2, 11.0, 0.5),
                visitor.statistics());
    }

    @Test
    void emptyDirectoryHasFiniteZeroStatistics() {
        ProjectStatisticsVisitor visitor = new ProjectStatisticsVisitor();
        root().accept(visitor);
        assertEquals(new ProjectStatisticsVisitor.Statistics(0, 1, 0, 0, 0, 0, 0, 0.0, 0.0), visitor.statistics());
    }

    @Test
    void averagesComplexityUsingFloatingPointAndSourceFilesOnly() {
        ProjectStatisticsVisitor visitor = new ProjectStatisticsVisitor();
        root(source("a", 3), source("b", 4), testFile("a")).accept(visitor);
        assertEquals(3.5, visitor.statistics().averageComplexity());
        assertEquals(0.5, visitor.statistics().testSourceRatio());
    }

    @Test
    void supportsLongTotalsBeyondIntegerRange() {
        ProjectStatisticsVisitor visitor = new ProjectStatisticsVisitor();
        root(new SourceCodeFile("a", "a", Integer.MAX_VALUE, Integer.MAX_VALUE, false, false),
                new SourceCodeFile("b", "b", Integer.MAX_VALUE, Integer.MAX_VALUE, false, false)).accept(visitor);
        assertEquals(4_294_967_294L, visitor.statistics().linesOfCode());
        assertEquals((double) Integer.MAX_VALUE, visitor.statistics().averageComplexity());
    }

    @Test
    void recursivelyProcessesOneHundredNestedDirectories() {
        ProjectElement nested = source("deep", 7);
        for (int i = 0; i < 100; i++) nested = new Directory("level" + i, List.of(nested));
        ProjectStatisticsVisitor visitor = new ProjectStatisticsVisitor();
        nested.accept(visitor);
        assertEquals(100, visitor.statistics().directories());
        assertEquals(1, visitor.statistics().files());
        assertEquals(100, visitor.statistics().linesOfCode());
    }

    @Test
    void securityDetectsAllFixtureRulesAndClampsScore() {
        SecurityAuditVisitor visitor = new SecurityAuditVisitor();
        DemoProjects.fintechProject().accept(visitor);
        assertEquals(5, visitor.criticalCount());
        assertEquals(1, visitor.warningCount());
        assertEquals(0, visitor.score());
        assertEquals(Set.of("EXPOSED_CREDENTIALS", "HIGH_RISK_SOURCE", "INSECURE_TRANSPORT",
                        "DEBUG_ENABLED", "VULNERABLE_DEPENDENCY"),
                visitor.issues().stream().map(Issue::rule).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void cleanProjectScoresOneHundred() {
        SecurityAuditVisitor visitor = new SecurityAuditVisitor();
        DemoProjects.releaseReadyProject().accept(visitor);
        assertTrue(visitor.issues().isEmpty());
        assertEquals(100, visitor.score());
    }

    @Test
    void credentialsInTestsAreAlsoCritical() {
        SecurityAuditVisitor visitor = new SecurityAuditVisitor();
        new TestFile("test", 1, 1, Set.of(), true).accept(visitor);
        assertEquals(1, visitor.criticalCount());
        assertEquals("EXPOSED_CREDENTIALS", visitor.issues().get(0).rule());
        assertEquals(80, visitor.score());
    }

    @Test
    void debugModeOnlyProducesAWarning() {
        SecurityAuditVisitor visitor = new SecurityAuditVisitor();
        new ConfigurationFile("app", 3, false, true, true).accept(visitor);
        assertEquals(0, visitor.criticalCount());
        assertEquals(1, visitor.warningCount());
        assertEquals(95, visitor.score());
    }

    @Test
    void securityIssueSnapshotsCannotBeMutated() {
        SecurityAuditVisitor visitor = new SecurityAuditVisitor();
        List<Issue> before = visitor.issues();
        new SourceCodeFile("bad", "bad", 1, 1, true, false).accept(visitor);
        assertTrue(before.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> visitor.issues().clear());
    }

    @Test
    void releaseReadyProjectPassesQualityGate() {
        QualityGateVisitor visitor = new QualityGateVisitor();
        DemoProjects.releaseReadyProject().accept(visitor);
        assertEquals(new QualityGateVisitor.Result(true, true, true, true, true), visitor.result());
    }

    @Test
    void riskyProjectFailsSecurityAndComplexityButPassesRatio() {
        QualityGateVisitor visitor = new QualityGateVisitor();
        DemoProjects.fintechProject().accept(visitor);
        assertEquals(new QualityGateVisitor.Result(false, false, true, true, false), visitor.result());
        assertTrue(visitor.report().contains("FINAL RESULT: FAILED"));
    }

    @Test
    void absentTestsFailQualityEvenWhenSecurityAndComplexityPass() {
        QualityGateVisitor visitor = new QualityGateVisitor();
        root(source("a", 2)).accept(visitor);
        assertEquals(new QualityGateVisitor.Result(true, true, false, true, false), visitor.result());
    }

    @Test
    void thresholdEqualityPassesAtTenAndPointFour() {
        QualityGateVisitor visitor = new QualityGateVisitor();
        root(source("a", 10), source("b", 10), source("c", 10), source("d", 10), source("e", 10),
                testFile("a"), testFile("b")).accept(visitor);
        assertTrue(visitor.result().passed());
    }

    @Test
    void emptyProjectCannotPassEvenWithZeroMinimumRatio() {
        QualityGateVisitor visitor = new QualityGateVisitor(10, 0);
        root().accept(visitor);
        assertFalse(visitor.result().sourcePresent());
        assertFalse(visitor.result().passed());
    }

    @Test
    void securityWarningDoesNotFailZeroCriticalGate() {
        QualityGateVisitor visitor = new QualityGateVisitor();
        root(source("a", 2), testFile("a"), new ConfigurationFile("app", 2, false, true, true)).accept(visitor);
        assertTrue(visitor.result().passed());
    }

    @Test
    void customQualityThresholdsChangeDecision() {
        Directory project = root(source("a", 11), testFile("a"));
        QualityGateVisitor standard = new QualityGateVisitor();
        QualityGateVisitor relaxed = new QualityGateVisitor(12, 0.5);
        project.accept(standard);
        project.accept(relaxed);
        assertFalse(standard.result().passed());
        assertTrue(relaxed.result().passed());
    }

    @Test
    void rejectsInvalidQualityThresholds() {
        assertThrows(IllegalArgumentException.class, () -> new QualityGateVisitor(Double.NaN, 0.4));
        assertThrows(IllegalArgumentException.class, () -> new QualityGateVisitor(10, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new QualityGateVisitor(-1, 0.4));
        assertThrows(IllegalArgumentException.class, () -> new QualityGateVisitor(10, -0.1));
    }

    @Test
    void complianceFindsMissingVersionsProhibitedDependenciesCredentialsAndUntestedModules() {
        ComplianceVisitor visitor = new ComplianceVisitor();
        DemoProjects.fintechProject().accept(visitor);
        assertFalse(visitor.passed());
        assertEquals(5, visitor.issues().size());
        assertEquals(1, visitor.issues().stream().filter(i -> i.rule().equals("UNTESTED_MODULE")).count());
        assertTrue(visitor.issues().stream().anyMatch(i -> i.rule().equals("MISSING_VERSION")));
        assertTrue(visitor.issues().stream().anyMatch(i -> i.rule().equals("PROHIBITED_DEPENDENCY")));
    }

    @Test
    void cleanProjectPassesCompliance() {
        ComplianceVisitor visitor = new ComplianceVisitor();
        DemoProjects.releaseReadyProject().accept(visitor);
        assertTrue(visitor.passed());
        assertTrue(visitor.issues().isEmpty());
    }

    @Test
    void complianceCoverageIsIndependentOfTraversalOrder() {
        ComplianceVisitor before = new ComplianceVisitor();
        ComplianceVisitor after = new ComplianceVisitor();
        root(testFile("a"), source("a", 2)).accept(before);
        root(source("a", 2), testFile("a")).accept(after);
        assertTrue(before.passed());
        assertEquals(before.issues(), after.issues());
        assertEquals(before.report(), before.report());
    }

    @Test
    void oneTestCanCoverSeveralFilesInTheSameModule() {
        ComplianceVisitor visitor = new ComplianceVisitor();
        root(source("payments", 2), new SourceCodeFile("Another.java", "payments", 2, 1, false, false),
                testFile("payments")).accept(visitor);
        assertTrue(visitor.passed());
    }

    @Test
    void missingAndBlankDependencyVersionsAreViolations() {
        ComplianceVisitor visitor = new ComplianceVisitor(Set.of("blocked:lib"));
        new DependencyFile("pom", 1, List.of(
                new Dependency("a:lib", null, null, false),
                new Dependency("blocked:lib", "   ", "MIT", false))).accept(visitor);
        assertEquals(3, visitor.issues().size());
        assertEquals(2, visitor.issues().stream().filter(i -> i.rule().equals("MISSING_VERSION")).count());
    }

    @Test
    void complianceDetectsTestCredentials() {
        ComplianceVisitor visitor = new ComplianceVisitor();
        new TestFile("test", 1, 1, Set.of(), true).accept(visitor);
        assertFalse(visitor.passed());
        assertEquals("EXPOSED_CREDENTIALS", visitor.issues().get(0).rule());
    }

    @Test
    void modelRejectsNegativeMetricsAndBlankNames() {
        assertThrows(IllegalArgumentException.class, () -> source("a", -1));
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationFile("app", -1, false, true, false));
        assertThrows(IllegalArgumentException.class, () -> new TestFile("t", 1, -1, Set.of(), false));
        assertThrows(IllegalArgumentException.class, () -> new Directory(" ", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Dependency("", "1", "MIT", false));
    }

    @Test
    void zeroTestCasesCannotClaimModuleCoverage() {
        assertThrows(IllegalArgumentException.class, () -> new TestFile("t", 1, 0, Set.of("a"), false));
        ComplianceVisitor visitor = new ComplianceVisitor();
        root(source("a", 1), new TestFile("empty", 0, 0, Set.of(), false)).accept(visitor);
        assertEquals("UNTESTED_MODULE", visitor.issues().get(0).rule());
    }

    @Test
    void directoryDefensivelyCopiesItsChildrenAndRejectsNullElements() {
        List<ProjectElement> children = new ArrayList<>();
        children.add(source("a", 2));
        Directory directory = new Directory("src", children);
        children.clear();
        assertEquals(1, directory.children().size());
        assertThrows(UnsupportedOperationException.class, () -> directory.children().clear());
        assertThrows(NullPointerException.class, () -> new Directory("src", java.util.Arrays.asList((ProjectElement) null)));
    }

    @Test
    void dependencyFileDefensivelyCopiesItsDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(new Dependency("a:b", "1", "MIT", false));
        DependencyFile file = new DependencyFile("pom", 1, dependencies);
        dependencies.clear();
        assertEquals(1, file.dependencies().size());
        assertThrows(UnsupportedOperationException.class, () -> file.dependencies().clear());
    }
}
