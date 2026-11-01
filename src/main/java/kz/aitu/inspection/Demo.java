package kz.aitu.inspection;

import kz.aitu.inspection.model.Directory;
import kz.aitu.inspection.visitor.*;

public final class Demo {
    private Demo() { }

    public static void main(String[] args) {
        inspect(DemoProjects.fintechProject());
        inspect(DemoProjects.releaseReadyProject());
    }

    private static void inspect(Directory project) {
        System.out.println("=== " + project.name() + " ===");
        // Every operation receives the exact same root object. Fresh visitors isolate runs.
        SecurityAuditVisitor security = new SecurityAuditVisitor();
        ProjectStatisticsVisitor statistics = new ProjectStatisticsVisitor();
        QualityGateVisitor quality = new QualityGateVisitor();
        ComplianceVisitor compliance = new ComplianceVisitor();
        project.accept(security);
        project.accept(statistics);
        project.accept(quality);
        project.accept(compliance);
        System.out.println(security.report());
        System.out.println(statistics.report());
        System.out.println(quality.report());
        System.out.println(compliance.report());
    }
}
