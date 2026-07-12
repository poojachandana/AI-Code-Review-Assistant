package com.aicode.review.service.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpotBugs performs bytecode analysis, which requires the project to be
 * compiled first. Since users upload raw source (files, zips, or pasted
 * snippets) that usually isn't compiled, this analyzer reproduces the most
 * common SpotBugs-style bug patterns directly on the source AST/text instead.
 *
 * If you want true bytecode-level SpotBugs analysis, compile the uploaded
 * project first (e.g. via `mvn compile`) and point the real `spotbugs`
 * dependency at the resulting `target/classes` directory - the dependency
 * is already included in pom.xml for that extension.
 */
@Slf4j
@Component
public class BugPatternAnalyzer {

    private static final Pattern HARDCODED_SECRET = Pattern.compile(
            "(?i)(password|secret|api[_-]?key|token)\\s*=\\s*\"[^\"]{3,}\"");

    private static final Pattern SQL_CONCAT = Pattern.compile(
            "(?i)(select|insert|update|delete)\\s.+\"\\s*\\+\\s*\\w+");

    public List<AnalysisFinding> analyze(List<Path> javaFiles) {
        List<AnalysisFinding> findings = new ArrayList<>();

        for (Path file : javaFiles) {
            if (!file.getFileName().toString().endsWith(".java")) continue;
            String fileName = file.getFileName().toString();

            try {
                String content = Files.readString(file);
                analyzeTextPatterns(content, fileName, findings);

                CompilationUnit cu = StaticJavaParser.parse(file);
                analyzeStringEquality(cu, fileName, findings);
                analyzeEmptyCatchBlocks(cu, fileName, findings);
                analyzeGenericCatch(cu, fileName, findings);
                analyzeMutablePublicFields(cu, fileName, findings);
                analyzePrintStackTrace(cu, fileName, findings);
                analyzeResourceLeaks(cu, fileName, findings);

            } catch (Exception e) {
                log.warn("Bug pattern analysis skipped for {}: {}", file, e.getMessage());
            }
        }

        return findings;
    }

    private void analyzeTextPatterns(String content, String fileName, List<AnalysisFinding> findings) {
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher secretMatcher = HARDCODED_SECRET.matcher(line);
            if (secretMatcher.find()) {
                findings.add(finding("HIGH", "SECURITY", fileName, i + 1,
                        "Possible hardcoded credential or secret",
                        "Storing passwords, API keys, or tokens directly in source code is a security risk.",
                        "Move secrets to environment variables, a vault, or a secrets manager."));
            }

            Matcher sqlMatcher = SQL_CONCAT.matcher(line);
            if (sqlMatcher.find()) {
                findings.add(finding("CRITICAL", "SECURITY", fileName, i + 1,
                        "Possible SQL injection via string concatenation",
                        "Building SQL queries by concatenating strings allows attacker-controlled input to alter query logic.",
                        "Use PreparedStatement / parameterized queries or an ORM (JPA/Hibernate) instead."));
            }
        }
    }

    private void analyzeStringEquality(CompilationUnit cu, String fileName, List<AnalysisFinding> findings) {
        cu.findAll(BinaryExpr.class).forEach(expr -> {
            if ((expr.getOperator() == BinaryExpr.Operator.EQUALS || expr.getOperator() == BinaryExpr.Operator.NOT_EQUALS)
                    && looksLikeStringComparison(expr.toString())) {
                findings.add(finding("MEDIUM", "BUG", fileName, expr.getBegin().map(p -> p.line).orElse(null),
                        "String compared using == instead of .equals()",
                        "Reference comparison (==) checks identity, not content equality, and can cause subtle bugs.",
                        "Use .equals() (or Objects.equals()) to compare String content."));
            }
        });
    }

    private boolean looksLikeStringComparison(String exprText) {
        // Heuristic: literal string on either side of == / != is almost always a bug
        return exprText.contains("\"") && (exprText.contains("==") || exprText.contains("!="));
    }

    private void analyzeEmptyCatchBlocks(CompilationUnit cu, String fileName, List<AnalysisFinding> findings) {
        cu.findAll(CatchClause.class).forEach(cc -> {
            if (cc.getBody().getStatements().isEmpty()) {
                findings.add(finding("HIGH", "BUG", fileName, cc.getBegin().map(p -> p.line).orElse(null),
                        "Empty catch block swallows exceptions",
                        "Silently ignoring an exception hides failures and makes debugging much harder.",
                        "At minimum log the exception; ideally handle it or rethrow a meaningful error."));
            }
        });
    }

    private void analyzeGenericCatch(CompilationUnit cu, String fileName, List<AnalysisFinding> findings) {
        cu.findAll(CatchClause.class).forEach(cc -> {
            ClassOrInterfaceType type = cc.getParameter().getType().isClassOrInterfaceType()
                    ? cc.getParameter().getType().asClassOrInterfaceType() : null;
            if (type != null && (type.getNameAsString().equals("Exception") || type.getNameAsString().equals("Throwable"))) {
                findings.add(finding("LOW", "CODE_SMELL", fileName, cc.getBegin().map(p -> p.line).orElse(null),
                        "Catching overly broad exception type: " + type.getNameAsString(),
                        "Catching generic Exception/Throwable can accidentally swallow unrelated errors (e.g. NullPointerException, OutOfMemoryError).",
                        "Catch the most specific exception type(s) your code can actually throw."));
            }
        });
    }

    private void analyzeMutablePublicFields(CompilationUnit cu, String fileName, List<AnalysisFinding> findings) {
        cu.findAll(FieldDeclaration.class).forEach(fd -> {
            if (fd.isPublic() && fd.isStatic() && !fd.isFinal()) {
                findings.add(finding("MEDIUM", "BUG", fileName, fd.getBegin().map(p -> p.line).orElse(null),
                        "Mutable public static field: " + fd.getVariables().get(0).getNameAsString(),
                        "Public mutable static state can be modified from anywhere, causing unpredictable behavior and thread-safety issues.",
                        "Make the field private with accessors, or make it 'final' if it shouldn't change."));
            }
        });
    }

    private void analyzePrintStackTrace(CompilationUnit cu, String fileName, List<AnalysisFinding> findings) {
        cu.findAll(MethodCallExpr.class).forEach(call -> {
            if (call.getNameAsString().equals("printStackTrace")) {
                findings.add(finding("LOW", "MAINTAINABILITY", fileName, call.getBegin().map(p -> p.line).orElse(null),
                        "printStackTrace() used instead of a logger",
                        "Printing directly to stderr bypasses log levels, formatting, and centralized log aggregation.",
                        "Use a logging framework (SLF4J/Logback) instead, e.g. log.error(\"message\", exception)."));
            }
            if (call.getNameAsString().equals("println") || call.getNameAsString().equals("print")) {
                call.getScope().ifPresent(scope -> {
                    if (scope.toString().equals("System.out") || scope.toString().equals("System.err")) {
                        findings.add(finding("INFO", "MAINTAINABILITY", fileName, call.getBegin().map(p -> p.line).orElse(null),
                                "System.out/System.err used for output",
                                "Console printing is fine for quick scripts but not for production services.",
                                "Replace with a structured logger for consistent, filterable log output."));
                    }
                });
            }
        });
    }

    private void analyzeResourceLeaks(CompilationUnit cu, String fileName, List<AnalysisFinding> findings) {
        cu.findAll(MethodDeclaration.class).forEach(method -> {
            boolean hasResourceCreation = method.toString().matches("(?s).*new (FileInputStream|FileOutputStream|FileReader|FileWriter|BufferedReader|Scanner|Socket|Connection)\\(.*");
            boolean hasTryWithResources = !method.findAll(TryStmt.class).isEmpty()
                    && method.findAll(TryStmt.class).stream().anyMatch(t -> !t.getResources().isEmpty());

            if (hasResourceCreation && !hasTryWithResources) {
                findings.add(finding("MEDIUM", "BUG", fileName, method.getBegin().map(p -> p.line).orElse(null),
                        "Resource may not be closed: " + method.getNameAsString() + "()",
                        "Opening streams/connections without try-with-resources can leak file handles or sockets if an exception occurs.",
                        "Wrap the resource in a try-with-resources block so it's closed automatically."));
            }
        });
    }

    private AnalysisFinding finding(String severity, String category, String fileName, Integer line,
                                     String issue, String explanation, String suggestion) {
        return AnalysisFinding.builder()
                .source("SPOTBUGS")
                .severity(severity)
                .category(category)
                .fileName(fileName)
                .lineNumber(line)
                .issue(issue)
                .explanation(explanation)
                .suggestion(suggestion)
                .build();
    }
}
