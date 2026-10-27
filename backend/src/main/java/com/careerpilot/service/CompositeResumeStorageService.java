package com.careerpilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class CompositeResumeStorageService implements ResumeStorageService {

    private static final Logger log = LoggerFactory.getLogger(CompositeResumeStorageService.class);

    @Value("${careerpilot.resume.storage-bucket:resumes}")
    private String bucketName;

    @Value("${careerpilot.resume.local-storage-path:storage/resumes}")
    private String localStoragePath;

    @Value("${careerpilot.resume.supabase-url:}")
    private String supabaseUrl;

    @Value("${careerpilot.resume.supabase-service-key:}")
    private String supabaseServiceKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String storeResume(UUID userId, UUID resumeId, String originalFilename, byte[] content) {
        String objectPath = userId.toString() + "/" + resumeId.toString() + ".pdf";

        if (isSupabaseStorageConfigured()) {
            try {
                storeInSupabaseStorage(objectPath, content);
                log.info("Successfully uploaded resume to Supabase Storage: bucket={}, path={}", bucketName, objectPath);
                return "supabase://" + bucketName + "/" + objectPath;
            } catch (Exception e) {
                log.warn("Failed to store resume in Supabase Storage ({}). Falling back to local storage.", e.getMessage());
            }
        } else {
            log.info("Supabase Storage credentials not configured. Using local filesystem storage.");
        }

        storeInLocalStorage(objectPath, content);
        return "local://" + objectPath;
    }

    @Override
    public byte[] loadResume(String storagePath) {
        if (storagePath.startsWith("supabase://")) {
            String path = storagePath.substring(("supabase://" + bucketName + "/").length());
            try {
                return loadFromSupabaseStorage(path);
            } catch (Exception e) {
                log.warn("Failed to load from Supabase Storage: {}. Attempting local fallback.", e.getMessage());
            }
        }

        String localPath = storagePath.startsWith("local://")
                ? storagePath.substring("local://".length())
                : storagePath;

        return loadFromLocalStorage(localPath);
    }

    @Override
    public void deleteResume(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        if (storagePath.startsWith("supabase://")) {
            String path = storagePath.substring(("supabase://" + bucketName + "/").length());
            try {
                deleteFromSupabaseStorage(path);
                log.info("Deleted resume from Supabase Storage: path={}", path);
            } catch (Exception e) {
                log.warn("Failed to delete from Supabase Storage: {}", e.getMessage());
            }
        }

        String localPath = storagePath.startsWith("local://")
                ? storagePath.substring("local://".length())
                : storagePath;

        deleteFromLocalStorage(localPath);
    }

    private boolean isSupabaseStorageConfigured() {
        return supabaseUrl != null && !supabaseUrl.isBlank() && !supabaseUrl.contains("your-project")
                && supabaseServiceKey != null && !supabaseServiceKey.isBlank();
    }

    private void storeInSupabaseStorage(String objectPath, byte[] content) {
        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String endpoint = cleanUrl + "/storage/v1/object/" + bucketName + "/" + objectPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.set("apikey", supabaseServiceKey);

        HttpEntity<byte[]> request = new HttpEntity<>(content, headers);
        ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Supabase Storage upload failed with status " + response.getStatusCode());
        }
    }

    private byte[] loadFromSupabaseStorage(String objectPath) {
        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String endpoint = cleanUrl + "/storage/v1/object/" + bucketName + "/" + objectPath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.set("apikey", supabaseServiceKey);

        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(endpoint, HttpMethod.GET, request, byte[].class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException("Supabase Storage read failed with status " + response.getStatusCode());
    }

    private void deleteFromSupabaseStorage(String objectPath) {
        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String endpoint = cleanUrl + "/storage/v1/object/" + bucketName + "/" + objectPath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.set("apikey", supabaseServiceKey);

        HttpEntity<?> request = new HttpEntity<>(headers);
        restTemplate.exchange(endpoint, HttpMethod.DELETE, request, String.class);
    }

    private void storeInLocalStorage(String objectPath, byte[] content) {
        try {
            Path targetFile = Paths.get(localStoragePath, objectPath);
            Files.createDirectories(targetFile.getParent());
            try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                fos.write(content);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist resume file to local storage: " + e.getMessage(), e);
        }
    }

    private byte[] loadFromLocalStorage(String objectPath) {
        try {
            Path targetFile = Paths.get(localStoragePath, objectPath);
            if (!Files.exists(targetFile)) {
                throw new RuntimeException("Resume file not found at " + targetFile);
            }
            return Files.readAllBytes(targetFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resume file from local storage: " + e.getMessage(), e);
        }
    }

    private void deleteFromLocalStorage(String objectPath) {
        try {
            Path targetFile = Paths.get(localStoragePath, objectPath);
            if (Files.exists(targetFile)) {
                Files.delete(targetFile);
            }
        } catch (IOException e) {
            log.warn("Failed to delete local resume file: {}", e.getMessage());
        }
    }
}
