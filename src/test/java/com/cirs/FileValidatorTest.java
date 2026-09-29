package com.cirs;

import com.cirs.exception.BadRequestException;
import com.cirs.util.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator();
    }

    @Test
    @DisplayName("Should accept valid plain text evidence file")
    void testValidTextFile() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "evidence.txt",
                "text/plain",
                "Log file line 1: Suspicious SSH login attempt".getBytes(StandardCharsets.UTF_8)
        );

        String mimeType = fileValidator.validateAndDetectMimeType(file);
        assertNotNull(mimeType);
        assertTrue(mimeType.contains("text/plain"));
    }

    @Test
    @DisplayName("Should reject prohibited file extensions such as .exe")
    void testProhibitedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "malware.exe",
                "application/octet-stream",
                "fake executable content".getBytes(StandardCharsets.UTF_8)
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileValidator.validateAndDetectMimeType(file));

        assertTrue(ex.getMessage().contains("not permitted"));
    }

    @Test
    @DisplayName("Should sanitize path traversal attempts in filename")
    void testPathTraversalSanitization() {
        String dangerousName = "../../../../etc/passwd";
        String cleanName = fileValidator.sanitizeFilename(dangerousName);

        assertFalse(cleanName.contains(".."));
        assertFalse(cleanName.contains("/"));
        assertEquals("passwd", cleanName);
    }

    @Test
    @DisplayName("Should sanitize Windows backslash path traversal attempts")
    void testWindowsPathTraversalSanitization() {
        String dangerousName = "..\\..\\Windows\\System32\\cmd.exe";
        String cleanName = fileValidator.sanitizeFilename(dangerousName);

        assertFalse(cleanName.contains(".."));
        assertFalse(cleanName.contains("\\"));
        assertEquals("cmd.exe", cleanName);
    }
}
