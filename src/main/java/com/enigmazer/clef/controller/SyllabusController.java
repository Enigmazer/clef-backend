package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.subject.common.SubjectSyllabusUrlResponse;
import com.enigmazer.clef.dto.subject.teacher.SubjectUpdateResponse;
import com.enigmazer.clef.dto.unit.UnitParseResponse;
import com.enigmazer.clef.service.Syllabus.SyllabusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/subjects/{subjectId}/syllabus")
@Tag(name = "Syllabus Management")
public class SyllabusController {

    private final SyllabusService syllabusService;

    @GetMapping
    @Operation(summary = "Get syllabus URL for the subject")
    public ResponseEntity<SubjectSyllabusUrlResponse> getSyllabusUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId
    ) {
        return ResponseEntity.ok(syllabusService.getSyllabusUrl(subjectId, principal.getId()));
    }

    @PutMapping
    @Operation(summary = "Upload the syllabus PDF for a subject")
    public ResponseEntity<SubjectUpdateResponse> uploadSyllabus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(syllabusService.uploadSyllabus(subjectId, file, principal.getId()));
    }

    @DeleteMapping
    @Operation(summary = "Delete the syllabus PDF for a subject")
    public ResponseEntity<SubjectUpdateResponse> deleteSyllabus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId
    ) {
        return ResponseEntity.ok(syllabusService.deleteSyllabus(subjectId, principal.getId()));
    }

    @GetMapping("/parse")
    @Operation(summary = "Parse units and topics from the syllabus PDF")
    public ResponseEntity<List<UnitParseResponse>> parseSyllabus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId
    ) {
        return ResponseEntity.ok(syllabusService.parseSyllabus(subjectId, principal.getId()));
    }
}
