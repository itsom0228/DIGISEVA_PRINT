package com.printwithai.storage;

import com.printwithai.exception.InvalidFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class StorageService {

    private final Path rootLocation;
    private final long maxFileSizeBytes;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/pjpeg"
    );

    public StorageService(
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            @Value("${app.upload.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
        init();
    }

    private void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory: " + rootLocation, e);
        }
    }

    public StoredFileInfo store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Cannot store an empty file.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidFileException("File size exceeds the configured maximum limit of " + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        String rawFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        // Security check against path traversal
        if (rawFilename.contains("..") || rawFilename.contains("/") || rawFilename.contains("\\")) {
            throw new InvalidFileException("Invalid filename containing path traversal sequences.");
        }

        String extension = getFileExtension(rawFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("Unsupported file type. Only PDF, JPG, JPEG, and PNG files are accepted.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException("Invalid file MIME type: " + contentType);
        }

        // Generate safe randomized storage filename
        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        Path destinationFile = this.rootLocation.resolve(storedFilename).normalize();

        // Extra check that destination is inside root location
        if (!destinationFile.getParent().equals(this.rootLocation)) {
            throw new InvalidFileException("Cannot store file outside current storage directory.");
        }

        try {
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded file: " + rawFilename, e);
        }

        return new StoredFileInfo(
                rawFilename,
                storedFilename,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize(),
                destinationFile
        );
    }

    public StoredFileInfo storeBytes(byte[] bytes, String originalFilename, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new InvalidFileException("Cannot store empty content bytes.");
        }
        if (bytes.length > maxFileSizeBytes) {
            throw new InvalidFileException("Generated file size exceeds maximum limit.");
        }

        String rawFilename = StringUtils.cleanPath(originalFilename != null ? originalFilename : "document.pdf");
        if (rawFilename.contains("..") || rawFilename.contains("/") || rawFilename.contains("\\")) {
            rawFilename = "application_document.pdf";
        }

        String extension = getFileExtension(rawFilename).toLowerCase();
        if (extension.isEmpty()) {
            extension = "pdf";
            rawFilename = rawFilename + ".pdf";
        }

        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        Path destinationFile = this.rootLocation.resolve(storedFilename).normalize();

        try {
            Files.write(destinationFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store generated file: " + rawFilename, e);
        }

        return new StoredFileInfo(
                rawFilename,
                storedFilename,
                contentType != null ? contentType : "application/pdf",
                bytes.length,
                destinationFile
        );
    }

    public Path load(String storedFilename) {
        // Prevent path traversal
        if (storedFilename.contains("..") || storedFilename.contains("/") || storedFilename.contains("\\")) {
            throw new InvalidFileException("Invalid stored filename.");
        }
        return rootLocation.resolve(storedFilename).normalize();
    }

    public Resource loadAsResource(String storedFilename) {
        try {
            Path file = load(storedFilename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new InvalidFileException("File not found or unreadable: " + storedFilename);
            }
        } catch (MalformedURLException e) {
            throw new InvalidFileException("Could not read file: " + storedFilename);
        }
    }

    public boolean deleteFile(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) return false;
        try {
            Path file = load(storedFilename);
            return Files.deleteIfExists(file);
        } catch (Exception e) {
            return false;
        }
    }

    public String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    public record StoredFileInfo(
            String originalFilename,
            String storedFilename,
            String contentType,
            long sizeBytes,
            Path filePath
    ) {}
}
