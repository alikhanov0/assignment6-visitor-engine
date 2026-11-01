package kz.aitu.inspection;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import kz.aitu.inspection.model.*;
import kz.aitu.inspection.visitor.LicenseAuditVisitor;
import static org.junit.jupiter.api.Assertions.*;

class LicenseAuditTest {
    @Test
    void detectsUnapprovedLicenseInOriginalProject() {
        LicenseAuditVisitor visitor = new LicenseAuditVisitor();
        DemoProjects.fintechProject().accept(visitor);
        assertEquals(2, visitor.checkedDependencies());
        assertFalse(visitor.passed());
        assertEquals(1, visitor.issues().size());
        assertEquals("UNAPPROVED_LICENSE", visitor.issues().get(0).rule());
        assertTrue(visitor.report().contains("GPL-3.0-only"));
    }

    @Test
    void approvedLicensesPass() {
        LicenseAuditVisitor visitor = new LicenseAuditVisitor();
        new DependencyFile("pom", 1, List.of(
                new Dependency("a:b", "1", "MIT", false),
                new Dependency("c:d", "2", "Apache-2.0", false))).accept(visitor);
        assertTrue(visitor.passed());
        assertEquals(2, visitor.checkedDependencies());
    }

    @Test
    void missingLicenseNeedsReviewAndBlocksAuditPass() {
        LicenseAuditVisitor visitor = new LicenseAuditVisitor();
        new DependencyFile("pom", 1, List.of(new Dependency("a:b", "1", null, false))).accept(visitor);
        assertFalse(visitor.passed());
        assertEquals("UNKNOWN_LICENSE", visitor.issues().get(0).rule());
    }

    @Test
    void traversesNestedDirectoriesAndIgnoresNonDependencyElements() {
        LicenseAuditVisitor visitor = new LicenseAuditVisitor();
        Directory project = new Directory("root", List.of(
                new SourceCodeFile("a", "a", 3, 1, false, false),
                new TestFile("t", 1, 1, Set.of("a"), false),
                new ConfigurationFile("c", 1, false, true, false),
                new Directory("nested", List.of(new DependencyFile("pom", 1, List.of(
                        new Dependency("a:b", "1", "MIT", false)))))));
        project.accept(visitor);
        assertEquals(1, visitor.checkedDependencies());
        assertTrue(visitor.passed());
    }

    @Test
    void emptyDirectoryProducesSuccessfulAuditWithZeroDependencies() {
        LicenseAuditVisitor visitor = new LicenseAuditVisitor();
        new Directory("empty", List.of()).accept(visitor);
        assertEquals(0, visitor.checkedDependencies());
        assertTrue(visitor.passed());
    }

    @Test
    void supportsCustomLicensePolicy() {
        LicenseAuditVisitor visitor = new LicenseAuditVisitor(Set.of("Company-Approved"));
        new DependencyFile("pom", 1, List.of(new Dependency("a:b", "1", "MIT", false))).accept(visitor);
        assertFalse(visitor.passed());
        assertThrows(UnsupportedOperationException.class, () -> visitor.issues().clear());
    }
}
