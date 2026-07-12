package com.aicode.review.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Complexity Analysis: number of classes, methods, cyclomatic complexity,
 * lines of code, average method length, and a maintainability index.
 *
 * Cyclomatic complexity per method = 1 + number of decision points
 * (if, for, while, do-while, case, catch, &&, ||, ternary).
 *
 * Maintainability Index uses a simplified heuristic (0-100 scale) derived from
 * cyclomatic complexity, method length, and lines of code. This trades perfect
 * fidelity with the classic Halstead-based formula for something fast and
 * dependency-free.
 */
@Slf4j
@Service
public class ComplexityAnalysisService {

    @Data
    @Builder
    public static class ComplexityResult {
        private int numClasses;
        private int numMethods;
        private int linesOfCode;
        private double avgMethodLength;
        private double cyclomaticComplexity; // average per method
        private double maintainabilityIndex; // 0-100
    }

    public ComplexityResult analyze(List<Path> javaFiles) {
        int totalClasses = 0;
        int totalMethods = 0;
        int totalLoc = 0;
        int totalMethodLines = 0;
        int totalComplexity = 0;

        for (Path file : javaFiles) {
            if (!file.getFileName().toString().endsWith(".java")) continue;

            try {
                String content = Files.readString(file);
                totalLoc += (int) content.lines().filter(l -> !l.isBlank()).count();

                CompilationUnit cu = StaticJavaParser.parse(file);

                List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
                totalClasses += classes.size();

                List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
                totalMethods += methods.size();

                for (MethodDeclaration m : methods) {
                    int lines = m.getRange().map(r -> r.getLineCount()).orElse(1);
                    totalMethodLines += lines;
                    totalComplexity += computeCyclomaticComplexity(m);
                }
            } catch (Exception e) {
                log.warn("Could not parse {} for complexity analysis: {}", file, e.getMessage());
            }
        }

        double avgMethodLength = totalMethods > 0 ? (double) totalMethodLines / totalMethods : 0;
        double avgComplexity = totalMethods > 0 ? (double) totalComplexity / totalMethods : 0;

        double maintainability = 100
                - (avgComplexity * 3.5)
                - (avgMethodLength * 0.4)
                - (Math.log(Math.max(totalLoc, 1)) * 2.0);
        maintainability = Math.max(0, Math.min(100, maintainability));

        return ComplexityResult.builder()
                .numClasses(totalClasses)
                .numMethods(totalMethods)
                .linesOfCode(totalLoc)
                .avgMethodLength(round(avgMethodLength))
                .cyclomaticComplexity(round(avgComplexity))
                .maintainabilityIndex(round(maintainability))
                .build();
    }

    private int computeCyclomaticComplexity(MethodDeclaration method) {
        int[] complexity = {1}; // base complexity

        method.findAll(IfStmt.class).forEach(s -> complexity[0]++);
        method.findAll(ForStmt.class).forEach(s -> complexity[0]++);
        method.findAll(ForEachStmt.class).forEach(s -> complexity[0]++);
        method.findAll(WhileStmt.class).forEach(s -> complexity[0]++);
        method.findAll(DoStmt.class).forEach(s -> complexity[0]++);
        method.findAll(SwitchEntry.class).forEach(s -> complexity[0]++);
        method.findAll(CatchClause.class).forEach(s -> complexity[0]++);
        method.findAll(ConditionalExpr.class).forEach(s -> complexity[0]++);
        method.findAll(BinaryExpr.class).forEach(b -> {
            if (b.getOperator() == BinaryExpr.Operator.AND || b.getOperator() == BinaryExpr.Operator.OR) {
                complexity[0]++;
            }
        });

        return complexity[0];
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
