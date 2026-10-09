package com.printwithai;

import com.printwithai.exception.InvalidFileException;
import com.printwithai.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    @TempDir
    Path tempUploadDir;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        // Config: temp dir, max size 5 MB (5242880 bytes)
        storageService = new StorageService(tempUploadDir.toString(), 5242880);
    }

    @Test
    @DisplayName("Store valid PDF file securely with randomized name")
    void testStoreValidPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Application.pdf",
                "application/pdf",
                "%PDF-1.4 sample content".getBytes()
        );

        StorageService.StoredFileInfo info = storageService.store(file);

        assertNotNull(info);
        assertEquals("Application.pdf", info.originalFilename());
        assertNotEquals("Application.pdf", info.storedFilename());
        assertTrue(info.storedFilename().endsWith(".pdf"));
        assertTrue(info.filePath().toFile().exists());
    }

    @Test
    @DisplayName("Reject unsupported file format (e.g., .exe, .sh)")
    void testRejectUnsupportedFormat() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "virus.exe",
                "application/x-msdownload",
                "binary data".getBytes()
        );

        Exception exc = assertThrows(InvalidFileException.class, () -> storageService.store(file));
        assertTrue(exc.getMessage().contains("Unsupported file type"));
    }

    @Test
    @DisplayName("Reject file exceeding configured maximum size")
    void testRejectExceededSize() {
        // Create 6 MB dummy byte array
        byte[] largeBytes = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeBytes
        );

        Exception exc = assertThrows(InvalidFileException.class, () -> storageService.store(file));
        assertTrue(exc.getMessage().contains("exceeds the configured maximum limit"));
    }

    @Test
    @DisplayName("Reject path traversal attempt in filename")
    void testPathTraversalPrevention() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../etc/passwd.pdf",
                "application/pdf",
                "content".getBytes()
        );

        assertThrows(InvalidFileException.class, () -> storageService.store(file));
    }
}
