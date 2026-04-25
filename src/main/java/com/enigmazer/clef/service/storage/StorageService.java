package com.enigmazer.clef.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {
    // --- Syllabus ---
    String generateSyllabusKey(MultipartFile file);

    void uploadSyllabus(MultipartFile file, String key);

    String generateSyllabusUrl(String key);

    void deleteSyllabus(String key);

    // --- Topic Material ---
    String generateTopicMaterialKey(MultipartFile file);

    void uploadTopicMaterial(MultipartFile file, String key);

    String generateTopicMaterialUrl(String key);

    void deleteTopicMaterials(List<String> keys);

    // --- User Avatar ---
    String generateAvatarKey(MultipartFile file);

    String generateAvatarUrl(String key);

    void uploadAvatar(MultipartFile file, String key);

    void deleteAvatar(String avatarUrl);

    // --- Others ---
    byte[] getBytes(String key);
}
