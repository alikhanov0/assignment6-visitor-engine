package kz.aitu.inspection;

import java.util.List;
import java.util.Set;
import kz.aitu.inspection.model.*;

/** Deterministic metadata fixtures, not a parser or live vulnerability database. */
public final class DemoProjects {
    private DemoProjects() { }

    public static Directory fintechProject() {
        return new Directory("fintech-project", List.of(
                new Directory("src", List.of(
                        new SourceCodeFile("PaymentService.java", "payments", 220, 14, true, true),
                        new SourceCodeFile("UserService.java", "users", 120, 8, false, false))),
                new Directory("tests", List.of(
                        new TestFile("PaymentServiceTest.java", 100, 8, Set.of("payments"), false))),
                new Directory("config", List.of(
                        new ConfigurationFile("application.properties", 30, true, false, true))),
                new DependencyFile("pom.xml", 60, List.of(
                        new Dependency("org.example:payments-sdk", "1.0.0", "Apache-2.0", true),
                        new Dependency("com.legacy:unsafe-crypto", "", "GPL-3.0-only", false)))));
    }

    public static Directory releaseReadyProject() {
        return new Directory("fintech-project-fixed", List.of(
                new Directory("src", List.of(
                        new SourceCodeFile("PaymentService.java", "payments", 180, 8, false, false),
                        new SourceCodeFile("UserService.java", "users", 120, 6, false, false))),
                new Directory("tests", List.of(
                        new TestFile("PaymentServiceTest.java", 100, 8, Set.of("payments"), false),
                        new TestFile("UserServiceTest.java", 80, 5, Set.of("users"), false))),
                new Directory("config", List.of(
                        new ConfigurationFile("application.properties", 30, false, true, false))),
                new DependencyFile("pom.xml", 60, List.of(
                        new Dependency("org.example:payments-sdk", "2.0.0", "Apache-2.0", false)))));
    }
}
