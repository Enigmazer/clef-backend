package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.service.topicMaterial.TopicMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("subjects/{subjectId}/units/{unitId}/topics/{topicId}/topic-materials")
@RequiredArgsConstructor
@Tag(name = "Topic Material Management")
public class TopicMaterialController {

    private final TopicMaterialService topicMaterialService;

    // --- Get One Operations
    @GetMapping("/{id}")
    @Operation(summary = "Get topic material url if you are teacher or student of the subject.")
    public ResponseEntity<Map<String, String>> getTopicMaterialUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @PathVariable Long topicId,
            @PathVariable("id") Long topicMaterialId
    ){
        String topicMaterialUrl = topicMaterialService
                .getTopicMaterialUrl(subjectId, unitId, topicId, topicMaterialId, principal.getId());
        return ResponseEntity.ok()
                .body(Map.of("topicMaterialUrl", topicMaterialUrl));
    }
    // --- Patch Operations ----
    @PostMapping
    @Operation(summary = "Upload the topic material for a topic")
    public ResponseEntity<Map<String,String>> uploadTopicMaterial(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @PathVariable Long topicId,
            @RequestParam("file") MultipartFile file
    ){
        String topicMaterialUrl = topicMaterialService
                .uploadTopicMaterial(subjectId, unitId, topicId, file, principal.getId());
        return ResponseEntity.ok()
                .body(Map.of("topicMaterialUrl", topicMaterialUrl));
    }

    // --- Delete Operations ---
    @DeleteMapping
    @Operation(summary = "Delete topic materials for a topic")
    public ResponseEntity<Void> deleteTopicMaterials(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @PathVariable Long topicId,
            @RequestBody List<Long> topicMaterialIds
    ) {
        topicMaterialService.deleteTopicMaterials(subjectId, unitId, topicId, topicMaterialIds, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
