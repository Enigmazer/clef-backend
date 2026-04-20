package com.enigmazer.clef.service.storage;

import com.enigmazer.clef.exception.BusinessException;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService{

    @Value("${supabase.storage.public-url}")
    private String publicUrl;

    @Value("${supabase.storage.files-bucket}")
    private String filesBucket;

    @Value("${supabase.storage.avatars-bucket}")
    private String avatarsBucket;

    private final String topicMaterialFolder = "topic-materials";
    private final String syllabusFolder = "syllabi";
    private final String avatarFolder = "avatars";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private final RestClient restClient;

    // --- Syllabus ---
    @Override
    public String generateSyllabusKey(MultipartFile file){
        return generateKey(syllabusFolder, file);
    }
    @Override
    public void uploadSyllabus(MultipartFile file, String key){
        uploadFile(
                file,
                2 * 1024 * 1024,
                Set.of("application/pdf"),
                filesBucket,
                key
        );
    }
    @Override
    public String generateSyllabusUrl(String key){
        return generateSignedUrl(key, filesBucket);
    }
    @Override
    public void deleteSyllabus(String key){
        deleteFile(key, filesBucket);
    }

    // --- Topic Material ---
    @Override
    public String generateTopicMaterialKey(MultipartFile file){
        return generateKey(topicMaterialFolder, file);
    }
    @Override
    public void uploadTopicMaterial(MultipartFile file, String key){
        uploadFile(
                file,
                50 * 1024 * 1024,
                // support audio, video, pdf, image, word document
                Set.of("audio/mpeg", "audio/mp4", "video/mp4", "video/x-matroska",
                        "application/pdf", "image/jpeg", "image/png", "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                filesBucket,
                key
        );
    }
    @Override
    public String generateTopicMaterialUrl(String key){
        return generateSignedUrl(key, filesBucket);
    }
    @Override
    public void deleteTopicMaterials(List<String> keys){
        deleteFiles(keys, filesBucket);
    }

    // --- User Avatar ---
    @Override
    public String generateAvatarKey(MultipartFile file){
        return generateKey(avatarFolder, file);
    }
    @Override
    public String generateAvatarUrl(String key){
        return publicUrl + "/" + avatarsBucket + "/" + key;
    }
    @Override
    public void uploadAvatar(MultipartFile file, String key){
        uploadFile(
                file,
                2 * 1024 * 1024,
                Set.of("image/jpeg", "image/png"),
                avatarsBucket,
                key
        );
    }
    @Override
    public void deleteAvatar(String avatarUrl){
        String key = avatarUrl.substring(
                avatarUrl.indexOf(avatarsBucket + "/") + avatarsBucket.length() + 1
        );
        deleteFile(key, avatarsBucket);
    }

    // --- Others ---
    @Override
    public byte[] getBytes(String key) {
        String url = generateSyllabusUrl(key);
        try {
            return restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException e) {
            log.error("Failed to fetch file from storage [key={}, reason={}]", key, e.getMessage());
            throw new BusinessException("Unable to process your file at this time. Please try again later.");
        }
    }

    // --- Main Storage Methods ---
    private void uploadFile(
            MultipartFile file,
            long maxBytes,
            Set<String> allowedTypes,
            String bucket,
            String key
    ){
        try {
            validateFile(file, maxBytes, allowedTypes);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            byte[] bytes = file.getBytes();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
        } catch (IOException e) {
            log.error("Failed to read file bytes", e);
            throw new StorageException("Failed to process file", e);
        } catch (SdkException e) {
            log.error("Failed to upload file to storage", e);
            throw new StorageException("Failed to upload file", e);
        }
    }

    private String generateSignedUrl(String key, String bucket){
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofHours(1))
                    .build();

            PresignedGetObjectRequest objectResponse = s3Presigner.presignGetObject(getObjectPresignRequest);

            return objectResponse.url().toString();
        } catch (SdkException e) {
            log.error("Failed to generate signed URL [key={}]", key, e);
            throw new StorageException("Failed to generate file URL", e);
        }
    }

    private void deleteFile(String key, String bucket){
        try {
            if (key == null) return;

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (SdkException e) {
            log.error("Failed to delete file [key={}]", key, e);
            throw new StorageException("Failed to delete file ", e);
        }
    }

    private void deleteFiles(List<String> keys, String bucket){
        try {
            if (keys == null) return;

            List<ObjectIdentifier> objectIdentifiers = keys.stream()
                    .filter(Objects::nonNull)
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();

            if (objectIdentifiers.isEmpty()) return;

            Delete delete = Delete.builder()
                    .objects(objectIdentifiers)
                    .build();

            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(delete)
                    .build();

            s3Client.deleteObjects(deleteObjectsRequest);
        } catch (SdkException e) {
            log.error("Failed to delete files in batch [keysCount={}]", keys.size(), e);
            throw new StorageException("Failed to delete files in batch ", e);
        }
    }

    // --- Helper Methods ---
    private String generateKey(String folder, MultipartFile file){
        return folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
    }

    private void validateFile(MultipartFile file, long maxBytes, Set<String> allowedTypes) {
        if (file.isEmpty())
            throw new InvalidRequestException("File is empty");

        if (file.getSize() > maxBytes)
            throw new InvalidRequestException("File too large");

        // TODO: add magic byte validation
        if (!allowedTypes.contains(file.getContentType()))
            throw new InvalidRequestException("File type not allowed");
    }
}
