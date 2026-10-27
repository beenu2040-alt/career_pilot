package com.careerpilot.service;

import java.util.UUID;

public interface ResumeStorageService {

    /**
     * Stores the PDF content and returns the non-guessable storage path.
     */
    String storeResume(UUID userId, UUID resumeId, String originalFilename, byte[] content);

    /**
     * Retrieves the raw PDF bytes from storage.
     */
    byte[] loadResume(String storagePath);

    /**
     * Deletes the stored resume from storage.
     */
    void deleteResume(String storagePath);
}
