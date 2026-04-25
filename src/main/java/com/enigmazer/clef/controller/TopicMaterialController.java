package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.topicMaterial.TopicMaterialDeleteRequest;
import com.enigmazer.clef.dto.topicMaterial.TopicMaterialResponse;
import com.enigmazer.clef.dto.topicMaterial.TopicMaterialUrlResponse;
import com.enigmazer.clef.service.topicMaterial.TopicMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("subjects/{subjectId}/units/{unitId}/topics/{topicId}/topic-materials")
@Tag(name = "Topic Material Management")
public class TopicMaterialController {

    private final TopicMaterialService topicMaterialService;

    // --- Get One Operations
    @GetMapping("/{id}")
    @Operation(summary = "Get topic material url if you are teacher or student of the subject")
    public ResponseEntity<TopicMaterialUrlResponse> getTopicMaterialUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @PathVariable Long topicId,
            @PathVariable("id") Long topicMaterialId
    ){
        return ResponseEntity.ok(topicMaterialService
                .getTopicMaterialUrl(subjectId, unitId, topicId, topicMaterialId, principal.getId()));
    }

    // --- Patch Operations ----
    @PostMapping
    @Operation(summary = "Upload the topic material for a topic")
    public ResponseEntity<TopicMaterialResponse> uploadTopicMaterial(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @PathVariable Long topicId,
            @RequestParam("file") MultipartFile file
    ){
        return ResponseEntity.ok(topicMaterialService
                .uploadTopicMaterial(subjectId, unitId, topicId, file, principal.getId()));
    }

    // --- Delete Operations ---
    @DeleteMapping
    @Operation(summary = "Delete topic materials for a topic")
    public ResponseEntity<Void> deleteTopicMaterials(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @PathVariable Long topicId,
            @Valid @RequestBody TopicMaterialDeleteRequest request
    ) {
        topicMaterialService.deleteTopicMaterials(subjectId, unitId, topicId, request, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
