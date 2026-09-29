package com.cirs.service.impl;

import com.cirs.entity.Incident;
import com.cirs.entity.IncidentAttachment;
import com.cirs.entity.User;
import com.cirs.exception.FileStorageException;
import com.cirs.exception.ResourceNotFoundException;
import com.cirs.repository.IncidentAttachmentRepository;
import com.cirs.service.FileStorageService;
import com.cirs.util.FileValidator;
import com.cirs.util.HashUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private final Path fileStorageLocation;
    private final FileValidator fileValidator;
    private final IncidentAttachmentRepository attachmentRepository;

    public FileStorageServiceImpl(@Value("${cirs.storage.upload-dir:./uploads/evidence}") String uploadDir,
                                  FileValidator fileValidator,
                                  IncidentAttachmentRepository attachmentRepository) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileValidator = fileValidator;
        this.attachmentRepository = attachmentRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("Evidence storage directory initialized at: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create directory for storing evidence files.", ex);
        }
    }

    @Override
    @Transactional
    public IncidentAttachment storeEvidence(MultipartFile file, Incident incident, User uploader) {
        // 1. Deep validation of file content, magic numbers and size using Apache Tika
        String detectedMimeType = fileValidator.validateAndDetectMimeType(file);

        // 2. Sanitize original filename
        String sanitizedOriginalName = fileValidator.sanitizeFilename(file.getOriginalFilename());

        // 3. Generate secure, non-enumerable stored filename
        String storedFilename = UUID.randomUUID().toString() + "_" + sanitizedOriginalName;

        Path targetLocation = this.fileStorageLocation.resolve(storedFilename).normalize();

        // Path traversal defense verification
        if (!targetLocation.startsWith(this.fileStorageLocation)) {
            throw new FileStorageException("Security violation: Attempted path traversal for file: " + sanitizedOriginalName);
        }

        // 4. Calculate SHA-256 hash for evidentiary chain-of-custody
        String sha256Hash;
        try (InputStream inputStream = file.getInputStream()) {
            sha256Hash = HashUtils.calculateSha256(inputStream);
        } catch (IOException e) {
            throw new FileStorageException("Failed to calculate SHA-256 hash for evidence: " + sanitizedOriginalName, e);
        }

        // 5. Store file on disk
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new FileStorageException("Could not store evidence file " + sanitizedOriginalName + ". Please try again!", ex);
        }

        // 6. Save metadata record to MySQL
        IncidentAttachment attachment = new IncidentAttachment(
                incident,
                sanitizedOriginalName,
                storedFilename,
                targetLocation.toString(),
                file.getSize(),
                detectedMimeType,
                sha256Hash,
                uploader
        );

        return attachmentRepository.save(attachment);
    }

    @Override
    public Resource loadFileAsResource(IncidentAttachment attachment) {
        try {
            Path filePath = this.fileStorageLocation.resolve(attachment.getStoredFilename()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Evidence file not found on disk: " + attachment.getOriginalFilename());
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Evidence file path resolution error: " + attachment.getOriginalFilename());
        }
    }
}
