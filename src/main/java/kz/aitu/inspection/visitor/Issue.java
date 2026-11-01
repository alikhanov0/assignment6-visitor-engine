package kz.aitu.inspection.visitor;

public record Issue(Severity severity, String location, String rule, String message) {
    public enum Severity { CRITICAL, WARNING }

    @Override
    public String toString() {
        return severity + " | " + location + " | " + rule + " | " + message;
    }
}
