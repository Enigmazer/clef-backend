package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.topic.TopicCompleteResponse;
import com.enigmazer.clef.dto.topic.TopicUpdateRequest;
import com.enigmazer.clef.dto.topic.TopicUpdateResponse;
import com.enigmazer.clef.service.topic.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("subjects/{subjectId}/units/{unitId}/topics")
@RequiredArgsConstructor
@Tag(name = "Topic Management")
public class TopicController {

    private final TopicService topicService;

    // --- Patch Operations ----
    @PatchMapping("/update")
    @Operation(summary = "Update the title of the topics.")
    public ResponseEntity<List<TopicUpdateResponse>> updateTopics(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @Valid @RequestBody List<TopicUpdateRequest> request
    ){
        return ResponseEntity.ok()
                .body(topicService.updateTopics(
                        subjectId, unitId, request, principal.getId()));
    }

    @PatchMapping("/{topicId}/complete/toggle")
    @Operation(summary = "Toggle the completion state of the topic.")
    public ResponseEntity<TopicCompleteResponse> toggleTopicComplete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @PathVariable Long topicId
    ){
        return ResponseEntity.ok()
                .body(topicService.toggleTopicComplete(
                        subjectId, unitId, topicId, principal.getId()));
    }

    // --- Delete Operations ---
    @DeleteMapping("/delete")
    @Operation(summary = "Delete topics.")
    public ResponseEntity<Void> deleteTopics(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @RequestBody List<Long> topicIds
    ){
        topicService.deleteTopics(subjectId, unitId, topicIds, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
