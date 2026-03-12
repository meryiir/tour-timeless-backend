package com.tourisme.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    private final Path uploadDir;
    
    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
            log.info("Upload directory created: {}", this.uploadDir);
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", this.uploadDir, e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }
    
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Validate file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name is null");
        }
        
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        
        // Validate image extensions
        String lowerExtension = extension.toLowerCase();
        if (!lowerExtension.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new IllegalArgumentException("Only image files (jpg, jpeg, png, gif, webp) are allowed");
        }
        
        // Generate unique filename
        String filename = UUID.randomUUID().toString() + extension;
        Path targetLocation = this.uploadDir.resolve(filename);
        
        // Copy file to target location
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("File stored successfully: {}", filename);
        return filename;
    }
    
    public Path loadFile(String filename) {
        return uploadDir.resolve(filename).normalize();
    }
    
    public void deleteFile(String filename) {
        try {
            Path filePath = loadFile(filename);
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", filename);
        } catch (IOException e) {
            log.error("Could not delete file: {}", filename, e);
        }
    }
}
