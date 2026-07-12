package com.aicode.review.service.analysis;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Wraps the Checkstyle Java API to run coding-standard checks against a set
 * of source files using Sun's well known checks (bundled inside the
 * checkstyle jar as a classpath resource).
 */
@Slf4j
@Component
public class CheckstyleAnalyzer {

    public List<AnalysisFinding> analyze(List<Path> javaFiles) {
        List<AnalysisFinding> findings = new ArrayList<>();

        try {
            Configuration config = ConfigurationLoader.loadConfiguration(
                    "/sun_checks.xml",
                    new PropertiesExpander(new Properties()));

            Checker checker = new Checker();
            checker.setModuleClassLoader(Checker.class.getClassLoader());
            checker.configure(config);

            checker.addListener(new AuditListener() {
                @Override
                public void auditStarted(AuditEvent event) {}

                @Override
                public void auditFinished(AuditEvent event) {}

                @Override
                public void fileStarted(AuditEvent event) {}

                @Override
                public void fileFinished(AuditEvent event) {}

                @Override
                public void addError(AuditEvent event) {
                    findings.add(AnalysisFinding.builder()
                            .source("CHECKSTYLE")
                            .category("STYLE")
                            .severity(mapSeverity(event.getSeverityLevel().getName()))
                            .fileName(new File(event.getFileName()).getName())
                            .lineNumber(event.getLine())
                            .issue(event.getMessage())
                            .explanation("Coding standard violation detected by Checkstyle (" + event.getSourceName() + ").")
                            .suggestion("Follow standard Java coding conventions (naming, whitespace, javadoc, imports).")
                            .build());
                }

                @Override
                public void addException(AuditEvent event, Throwable throwable) {
                    log.warn("Checkstyle exception on {}: {}", event.getFileName(), throwable.getMessage());
                }
            });

            List<File> files = javaFiles.stream().map(Path::toFile).toList();
            checker.process(files);
            checker.destroy();

        } catch (Exception e) {
            log.error("Checkstyle analysis failed: {}", e.getMessage(), e);
            findings.add(AnalysisFinding.builder()
                    .source("CHECKSTYLE")
                    .category("STYLE")
                    .severity("INFO")
                    .issue("Checkstyle analysis could not complete")
                    .explanation(e.getMessage())
                    .suggestion("Verify the uploaded files are valid, parseable Java source.")
                    .build());
        }

        return findings;
    }

    private String mapSeverity(String checkstyleSeverity) {
        return switch (checkstyleSeverity.toLowerCase()) {
            case "error" -> "HIGH";
            case "warning" -> "MEDIUM";
            case "info" -> "LOW";
            default -> "INFO";
        };
    }
}
