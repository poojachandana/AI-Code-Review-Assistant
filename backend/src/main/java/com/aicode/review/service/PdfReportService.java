package com.aicode.review.service;

import com.aicode.review.dto.ReviewFindingDTO;
import com.aicode.review.dto.ReviewResponseDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import java.awt.Color;

import java.io.ByteArrayOutputStream;

/** Export Reports (Optional feature): PDF, HTML, and Markdown renderers for a review. */
@Service
public class PdfReportService {

    public byte[] generatePdf(ReviewResponseDTO review) throws DocumentException {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font smallGray = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

        document.add(new Paragraph("AI Code Review Report", titleFont));
        document.add(new Paragraph(review.getProjectName(), smallGray));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Overall Code Quality Score: " + review.getReviewScore() + " / 100", headingFont));
        document.add(new Paragraph(review.getSummary(), normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Complexity Metrics", headingFont));
        document.add(new Paragraph(String.format(
                "Classes: %d | Methods: %d | Lines of Code: %d | Avg Method Length: %.2f | " +
                "Cyclomatic Complexity: %.2f | Maintainability Index: %.2f",
                review.getNumClasses(), review.getNumMethods(), review.getLinesOfCode(),
                review.getAvgMethodLength(), review.getCyclomaticComplexity(), review.getMaintainabilityIndex()
        ), normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Findings (" + review.getFindings().size() + ")", headingFont));
        for (ReviewFindingDTO f : review.getFindings()) {
            Paragraph p = new Paragraph();
            p.add(new Chunk("[" + f.getSeverity() + "] ", boldColorFont(f.getSeverity())));
            p.add(new Chunk(f.getIssue() + " ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            p.add(new Chunk("(" + f.getSource() + (f.getFileName() != null ? ", " + f.getFileName() : "")
                    + (f.getLineNumber() != null ? ":" + f.getLineNumber() : "") + ")", smallGray));
            document.add(p);
            if (f.getExplanation() != null) document.add(new Paragraph("  Why: " + f.getExplanation(), normalFont));
            if (f.getSuggestion() != null) document.add(new Paragraph("  Fix: " + f.getSuggestion(), normalFont));
            document.add(Chunk.NEWLINE);
        }

        document.close();
        return out.toByteArray();
    }

    private Font boldColorFont(String severity) {
        Color color = switch (severity) {
            case "CRITICAL" -> new Color(178, 24, 24);
            case "HIGH" -> new Color(214, 90, 30);
            case "MEDIUM" -> new Color(200, 160, 20);
            case "LOW" -> new Color(60, 120, 60);
            default -> Color.GRAY;
        };
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        f.setColor(color);
        return f;
    }

    public String generateHtml(ReviewResponseDTO review) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Code Review Report</title>")
          .append("<style>body{font-family:sans-serif;max-width:800px;margin:40px auto;color:#222}")
          .append("h1{margin-bottom:0} .meta{color:#777;margin-top:4px}")
          .append(".finding{border-left:4px solid #ccc;padding:8px 12px;margin:10px 0;background:#fafafa}")
          .append(".CRITICAL{border-color:#b21818}.HIGH{border-color:#d65a1e}.MEDIUM{border-color:#c8a014}")
          .append(".LOW{border-color:#3c783c}.INFO{border-color:#999}</style></head><body>");

        sb.append("<h1>AI Code Review Report</h1>")
          .append("<p class='meta'>").append(escape(review.getProjectName())).append("</p>")
          .append("<h2>Quality Score: ").append(review.getReviewScore()).append("/100</h2>")
          .append("<p>").append(escape(review.getSummary())).append("</p>")
          .append("<h3>Complexity Metrics</h3><ul>")
          .append("<li>Classes: ").append(review.getNumClasses()).append("</li>")
          .append("<li>Methods: ").append(review.getNumMethods()).append("</li>")
          .append("<li>Lines of Code: ").append(review.getLinesOfCode()).append("</li>")
          .append("<li>Avg Method Length: ").append(review.getAvgMethodLength()).append("</li>")
          .append("<li>Cyclomatic Complexity: ").append(review.getCyclomaticComplexity()).append("</li>")
          .append("<li>Maintainability Index: ").append(review.getMaintainabilityIndex()).append("</li>")
          .append("</ul>");

        sb.append("<h3>Findings (").append(review.getFindings().size()).append(")</h3>");
        for (ReviewFindingDTO f : review.getFindings()) {
            sb.append("<div class='finding ").append(f.getSeverity()).append("'>")
              .append("<strong>[").append(f.getSeverity()).append("] ").append(escape(f.getIssue())).append("</strong>")
              .append(" <em>(").append(f.getSource()).append(", ").append(escape(String.valueOf(f.getFileName())))
              .append(f.getLineNumber() != null ? ":" + f.getLineNumber() : "").append(")</em>")
              .append("<p><b>Why:</b> ").append(escape(f.getExplanation())).append("</p>")
              .append("<p><b>Fix:</b> ").append(escape(f.getSuggestion())).append("</p>")
              .append("</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    public String generateMarkdown(ReviewResponseDTO review) {
        StringBuilder sb = new StringBuilder();
        sb.append("# AI Code Review Report\n\n")
          .append("**Project:** ").append(review.getProjectName()).append("\n\n")
          .append("## Quality Score: ").append(review.getReviewScore()).append("/100\n\n")
          .append(review.getSummary()).append("\n\n")
          .append("## Complexity Metrics\n")
          .append("- Classes: ").append(review.getNumClasses()).append("\n")
          .append("- Methods: ").append(review.getNumMethods()).append("\n")
          .append("- Lines of Code: ").append(review.getLinesOfCode()).append("\n")
          .append("- Avg Method Length: ").append(review.getAvgMethodLength()).append("\n")
          .append("- Cyclomatic Complexity: ").append(review.getCyclomaticComplexity()).append("\n")
          .append("- Maintainability Index: ").append(review.getMaintainabilityIndex()).append("\n\n")
          .append("## Findings (").append(review.getFindings().size()).append(")\n\n");

        for (ReviewFindingDTO f : review.getFindings()) {
            sb.append("### [").append(f.getSeverity()).append("] ").append(f.getIssue()).append("\n")
              .append("- **Source:** ").append(f.getSource()).append("\n")
              .append("- **Location:** ").append(f.getFileName())
              .append(f.getLineNumber() != null ? ":" + f.getLineNumber() : "").append("\n")
              .append("- **Why:** ").append(f.getExplanation()).append("\n")
              .append("- **Fix:** ").append(f.getSuggestion()).append("\n\n");
        }

        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
