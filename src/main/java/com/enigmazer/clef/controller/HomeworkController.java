package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.homework.HomeWorkUpdateRequest;
import com.enigmazer.clef.dto.homework.HomeworkResponse;
import com.enigmazer.clef.service.homeWork.HomeWorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("subjects/{subjectId}/homework")
@Tag(name = "Homework Management")
public class HomeworkController {

    private final HomeWorkService homeWorkService;

    // --- Post Operations ---
    @PostMapping("/{id}")
    @Operation(summary = "Update homework")
    public ResponseEntity<HomeworkResponse> UpdateHomeWork(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable("id") Long homeWorkId,
            @Valid @RequestBody HomeWorkUpdateRequest request

            ) {
        return ResponseEntity.ok().body(
                homeWorkService.updateHomeWork(subjectId, homeWorkId, request, principal.getId())
        );
    }

    // --- Delete Operations ---
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete homework")
    public ResponseEntity<Void> deleteHomeWork(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable("id") Long homeWorkId
    ) {
        homeWorkService.deleteHomeWork(subjectId, homeWorkId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
