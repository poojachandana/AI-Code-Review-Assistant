package com.aicode.review.service;

import com.aicode.review.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Stage 1: File Processing.
 * Extracts uploaded projects, scans supported source files, and ignores
 * unsupported files such as images, binaries, build folders, and dependency folders.
 */
@Slf4j
@Service
public class FileProcessingService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".jsx", ".ts", ".tsx"
    );

    private static final Set<String> IGNORED_DIR_NAMES = Set.of(
            "target", "build", "node_modules", ".git", ".idea", ".vscode",
            "dist", "out", "bin", "__pycache__", ".mvn", "vendor"
    );

    /** Persist the raw upload (file or zip) to disk under a unique project folder. */
    public Path storeUpload(MultipartFile file, Long userId) throws IOException {
        String safeName = Paths.get(file.getOriginalFilename()).getFileName().toString();
        Path userDir = Paths.get(uploadDir, "user_" + userId, UUID.randomUUID().toString());
        Files.createDirectories(userDir);
        Path target = userDir.resolve(safeName);
        file.transferTo(target);
        return target;
    }

    /** Persist a pasted code snippet as a single file. */
    public Path storeSnippet(String code, String fileName, Long userId) throws IOException {
        String safeName = Paths.get(fileName).getFileName().toString();
        Path userDir = Paths.get(uploadDir, "user_" + userId, UUID.randomUUID().toString());
        Files.createDirectories(userDir);
        Path target = userDir.resolve(safeName);
        Files.writeString(target, code);
        return target;
    }

    /**
     * Given a stored path (a single source file OR a .zip archive), return the list
     * of supported source files to analyze, ignoring build/dependency folders.
     */
    public List<Path> extractSupportedSourceFiles(Path storedPath) throws IOException {
        List<Path> result = new ArrayList<>();

        if (storedPath.toString().toLowerCase().endsWith(".zip")) {
            Path extractDir = storedPath.getParent().resolve("extracted");
            Files.createDirectories(extractDir);
            unzip(storedPath, extractDir);
            collectSourceFiles(extractDir, result);
        } else {
            if (isSupported(storedPath)) {
                result.add(storedPath);
            } else {
                throw new BadRequestException("Unsupported file type: " + storedPath.getFileName());
            }
        }

        if (result.isEmpty()) {
            throw new BadRequestException("No supported source files (.java) found in upload");
        }

        return result;
    }

    private void collectSourceFiles(Path dir, List<Path> result) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> !isInIgnoredDir(dir, p))
                  .filter(this::isSupported)
                  .forEach(result::add);
        }
    }

    private boolean isInIgnoredDir(Path root, Path file) {
        Path relative = root.relativize(file);
        for (Path part : relative) {
            if (IGNORED_DIR_NAMES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSupported(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private void unzip(Path zipFile, Path targetDir) throws IOException {
        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolvedPath = targetDir.resolve(entry.getName()).normalize();

                // Zip-slip protection
                if (!resolvedPath.startsWith(targetDir)) {
                    throw new IOException("Invalid zip entry (potential path traversal): " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zis, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}
