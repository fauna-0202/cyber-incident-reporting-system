package com.cirs.util;

import com.cirs.exception.BadRequestException;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class FileValidator {

    private final Tika tika = new Tika();

    @Value("${cirs.storage.max-file-size-bytes:15728640}")
    private long maxFileSizeBytes = 15728640L; // 15MB default

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "png", "jpg", "jpeg", "txt", "docx", "zip", "pcap", "pcapng", "eml"
    ));

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "text/plain",
            "application/zip",
            "application/x-zip-compressed",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.tcpdump.pcap",
            "application/octet-stream",
            "message/rfc822"
    );

    public FileValidator() {}

    public FileValidator(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /**
     * Validates file size, extension, and deep magic-number MIME type using Apache Tika.
     */
    public String validateAndDetectMimeType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded evidence file cannot be empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File size exceeds maximum permitted limit of 15MB");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);

        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("File extension ." + extension + " is not permitted for evidence upload. Allowed: " + ALLOWED_EXTENSIONS);
        }

        // Deep inspection of magic bytes using Apache Tika
        try (InputStream inputStream = file.getInputStream()) {
            String detectedMimeType = tika.detect(inputStream, originalFilename);
            
            boolean isAllowed = ALLOWED_MIME_TYPES.stream()
                    .anyMatch(allowed -> allowed.equalsIgnoreCase(detectedMimeType));

            if (!isAllowed) {
                throw new BadRequestException("Security alert: File content signature (" + detectedMimeType + 
                        ") does not match permitted evidence formats.");
            }
            return detectedMimeType;
        } catch (IOException e) {
            throw new BadRequestException("Failed to inspect evidence file header: " + e.getMessage());
        }
    }

    /**
     * Sanitize filename to prevent directory traversal attacks (e.g. ../../)
     */
    public String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed_evidence.bin";
        }
        // Strip any path components (both UNIX and Windows)
        String cleanName = filename.replaceAll(".*[\\\\/]", "").trim();
        // Remove null bytes and hazardous characters
        cleanName = cleanName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleanName.isEmpty() || cleanName.equals(".")) {
            return "evidence.bin";
        }
        return cleanName;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}
