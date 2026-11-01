package kz.aitu.inspection.visitor;

import java.util.Locale;
import kz.aitu.inspection.model.*;

public final class ProjectStatisticsVisitor extends RecursiveProjectVisitor {
    public record Statistics(long files, long directories, long sourceFiles, long testFiles,
                             long testCases, long linesOfCode, long dependencies,
                             double averageComplexity, double testSourceRatio) { }

    private long files, directories, sources, tests, testCases, lines, dependencies, complexity;

    @Override
    public void visit(SourceCodeFile file) {
        files++;
        sources++;
        lines += file.linesOfCode();
        complexity += file.complexity();
    }

    @Override
    public void visit(TestFile file) {
        files++;
        tests++;
        lines += file.linesOfCode();
        testCases += file.testCases();
    }

    @Override
    public void visit(ConfigurationFile file) {
        files++;
        lines += file.linesOfCode();
    }

    @Override
    public void visit(DependencyFile file) {
        files++;
        lines += file.linesOfCode();
        dependencies += file.dependencies().size();
    }

    @Override
    public void visit(Directory directory) {
        directories++;
        super.visit(directory);
    }

    public Statistics statistics() {
        return new Statistics(files, directories, sources, tests, testCases, lines, dependencies,
                sources == 0 ? 0.0 : (double) complexity / sources,
                sources == 0 ? 0.0 : (double) tests / sources);
    }

    public String report() {
        Statistics s = statistics();
        return String.format(Locale.ROOT,
                "PROJECT STATISTICS%nFiles: %d (source: %d, test: %d)%nDirectories: %d%n"
                + "Lines of code: %d%nTest cases: %d%nDependencies: %d%n"
                + "Average source complexity: %.2f%nTest/source file ratio: %.2f%n",
                s.files(), s.sourceFiles(), s.testFiles(), s.directories(), s.linesOfCode(),
                s.testCases(), s.dependencies(), s.averageComplexity(), s.testSourceRatio());
    }
}
