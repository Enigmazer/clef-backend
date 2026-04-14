package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.enrollment.EnrolledStudentResponse;
import com.enigmazer.clef.dto.subject.*;
import com.enigmazer.clef.dto.unit.UnitCreationRequest;
import com.enigmazer.clef.service.subject.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/subjects")
@Tag(name = "Subject Management")
public class SubjectController {

    private final SubjectService subjectService;

    // --- Create Operations ---
    @PostMapping
    @Operation(summary = "Create a new subject")
    public ResponseEntity<SubjectDetailsTeacherResponse> createSubject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SubjectCreationRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectService.createSubject(request, principal.getId()));
    }

    // --- Get all Operations ---
    @GetMapping("/teacher")
    @Operation(summary = "List all the unarchived subjects created by the teacher")
    public ResponseEntity<List<ListTeacherSubjectsResponse>> listTeacherSubjects(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok()
                .body(subjectService.listTeacherSubjects(principal.getId()));
    }

    @GetMapping("/archived/teacher")
    @Operation(summary = "List all the archived subjects created by the teacher")
    public ResponseEntity<List<ListTeacherSubjectsResponse>> listArchivedTeacherSubjects(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok()
                .body(subjectService.listArchivedTeacherSubjects(principal.getId()));
    }

    @GetMapping("/student")
    @Operation(summary = "List all the unarchived subjects that the student is enrolled in")
    public ResponseEntity<List<ListStudentSubjectsResponse>> listStudentSubjects(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok()
                .body(subjectService.listStudentSubjects(principal.getId()));
    }

    @GetMapping("/{id}/enrolled/students")
    @Operation(summary = "List all the students enrolled in the subject")
    public ResponseEntity<List<EnrolledStudentResponse>> listEnrolledStudents(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        return ResponseEntity.ok()
                .body(subjectService.listEnrolledStudents(subjectId, principal.getId()));
    }

    // --- Get one Operations ---
    @GetMapping("/{id}/teacher")
    @Operation(summary = "Get subject details for the teacher")
    public ResponseEntity<SubjectDetailsTeacherResponse> getTeacherSubjectDetails(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        return ResponseEntity.ok()
                .body(subjectService.getTeacherSubjectDetails(subjectId, principal.getId()));
    }

    @GetMapping("/{id}/student")
    @Operation(summary = "Get subject details for the student if the subject is unarchived")
    public ResponseEntity<SubjectDetailsStudentResponse> getStudentSubjectDetails(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ){
        return ResponseEntity.ok()
                .body(subjectService.getStudentSubjectDetails(subjectId, principal.getId()));
    }

    // --- Patch Operations ---
    @PatchMapping("/{id}")
    @Operation(summary = "Update the name or description of the subject")
    public ResponseEntity<SubjectUpdateReqRes> updateSubject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId,
            @Valid @RequestBody SubjectUpdateReqRes request
    ){
        return ResponseEntity.ok()
                .body(subjectService.updateSubject(subjectId, request, principal.getId()));
    }

    @PatchMapping("/{id}/preferences/lock/toggle")
    @Operation(summary = "Toggle the lock state of a subject")
    public ResponseEntity<Boolean> toggleSubjectLock(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ){
        return ResponseEntity.ok()
                .body(subjectService.toggleSubjectLock(subjectId, principal.getId()));
    }

    @PatchMapping("/{id}/preferences/archive/toggle")
    @Operation(summary = "Toggle the archive state of a subject")
    public ResponseEntity<Boolean> toggleSubjectArchive(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ){
        return ResponseEntity.ok()
                .body(subjectService.toggleSubjectArchive(subjectId, principal.getId()));
    }

    @PatchMapping("/{id}/syllabus")
    @Operation(summary = "Set the syllabus URL for a subject")
    public ResponseEntity<Map<String,String>> setSyllabusUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId,
            @Valid @RequestBody SubjectAddSyllabusUrlRequest request
    ){
        String syllabusUrl = subjectService
                .setSyllabusUrl(subjectId, request.syllabusUrl(), principal.getId());
        return ResponseEntity.ok()
                .body(Map.of("syllabusUrl", syllabusUrl));
    }

    @PatchMapping("/{id}/current-topic")
    @Operation(summary = "Set currently teaching topic for subject")
    public ResponseEntity<SubjectCurrentNextTopicResponse> setCurrentTopic(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId,
            @Valid @RequestBody SubjectCurrentNextTopicRequest request
    ){
        return ResponseEntity.ok()
                .body(subjectService.setCurrentTopic(subjectId, request, principal.getId()));
    }

    @PatchMapping("/{id}/next-topic")
    @Operation(summary = "Set next topic taught for subject")
    public ResponseEntity<SubjectCurrentNextTopicResponse> setNextTopic(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId,
            @Valid @RequestBody SubjectCurrentNextTopicRequest request
    ){
        return ResponseEntity.ok()
                .body(subjectService.setNextTopic(subjectId, request, principal.getId()));
    }

    // --- Delete Operations ---
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a subject")
    public ResponseEntity<Void> deleteSubject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        subjectService.deleteSubject(subjectId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // --- sub-resource Operations ---
    @PostMapping("/join")
    @Operation(summary = "Join or enroll in subject using 6 Character unique join code")
    public ResponseEntity<SubjectDetailsStudentResponse> joinSubject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SubjectJoinRequest request
    ) {
        return ResponseEntity.ok()
                .body(subjectService.joinSubject(request.joinCode(), principal.getId()));
    }

    @PostMapping("/{id}/units/bulk")
    @Operation(summary = "Bulk add units and there topics in the subject")
    public ResponseEntity<SubjectDetailsTeacherResponse> bulkAddUnits(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId,
            @NotEmpty(message = "At least one unit is required.")
            @Valid @RequestBody List<UnitCreationRequest> request
    ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectService.bulkAddUnits(subjectId, request, principal.getId()));
    }
}
