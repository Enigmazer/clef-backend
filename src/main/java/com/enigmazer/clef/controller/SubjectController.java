package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.subject.common.SubjectUpdatedAtResponse;
import com.enigmazer.clef.dto.subject.student.SubjectDetailsStudentResponse;
import com.enigmazer.clef.dto.subject.student.SubjectJoinRequest;
import com.enigmazer.clef.dto.subject.student.SubjectsSummaryStudentResponse;
import com.enigmazer.clef.dto.subject.teacher.*;
import com.enigmazer.clef.dto.user.TeacherProfileResponse;
import com.enigmazer.clef.service.subject.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/subjects")
@Tag(name = "Subject Management")
public class SubjectController {

    private final SubjectService subjectService;

    // --- Subject Core (Teacher) ---
    @PostMapping
    @Operation(summary = "Create a new subject")
    public ResponseEntity<SubjectDetailsTeacherResponse> createSubject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SubjectCreationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectService.createSubject(request, principal.getId()));
    }

    @GetMapping("/teacher")
    @Operation(summary = "List all unarchived subjects created by the teacher")
    public ResponseEntity<List<SubjectsSummaryTeacherResponse>> listTeacherSubjects(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(subjectService.listTeacherSubjects(principal.getId()));
    }

    @GetMapping("/teacher/archived")
    @Operation(summary = "List all archived subjects created by the teacher")
    public ResponseEntity<List<SubjectsSummaryTeacherResponse>> listArchivedTeacherSubjects(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(subjectService.listArchivedTeacherSubjects(principal.getId()));
    }

    @GetMapping("/{id}/teacher")
    @Operation(summary = "Get subject details for the teacher")
    public ResponseEntity<SubjectDetailsTeacherResponse> getTeacherSubjectDetails(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        return ResponseEntity.ok(subjectService.getTeacherSubjectDetails(subjectId, principal.getId()));
    }

    // --- Subject Preferences ---
    @PatchMapping("/{id}")
    @Operation(summary = "Update the name or description of the subject")
    public ResponseEntity<SubjectUpdateResponse> updateSubject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId,
            @Valid @RequestBody SubjectUpdateRequest request
    ) {
        return ResponseEntity.ok(subjectService.updateSubject(subjectId, request, principal.getId()));
    }

    @PatchMapping("/{id}/preferences/lock/toggle")
    @Operation(summary = "Toggle the lock state of a subject")
    public ResponseEntity<SubjectUpdateResponse> toggleSubjectLock(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        return ResponseEntity.ok(subjectService.toggleSubjectLock(subjectId, principal.getId()));
    }

    @PatchMapping("/{id}/preferences/archive/toggle")
    @Operation(summary = "Toggle the archive state of a subject")
    public ResponseEntity<SubjectUpdateResponse> toggleSubjectArchive(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        return ResponseEntity.ok(subjectService.toggleSubjectArchive(subjectId, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a subject")
    public ResponseEntity<Void> deleteSubject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        subjectService.deleteSubject(subjectId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // --- Topic Progress ---
    @PatchMapping("/{id}/current-topic")
    @Operation(summary = "Set the currently teaching topic for the subject")
    public ResponseEntity<SubjectUpdateResponse> setCurrentTopic(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId,
            @Valid @RequestBody SubjectCurrentNextTopicRequest request
    ) {
        return ResponseEntity.ok(subjectService.setCurrentTopic(subjectId, request, principal.getId()));
    }

    @PatchMapping("/{id}/next-topic")
    @Operation(summary = "Set the next topic to be taught for the subject")
    public ResponseEntity<SubjectUpdateResponse> setNextTopic(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId,
            @Valid @RequestBody SubjectCurrentNextTopicRequest request
    ) {
        return ResponseEntity.ok(subjectService.setNextTopic(subjectId, request, principal.getId()));
    }

    // --- Subject (Student) ---
    @GetMapping("/student")
    @Operation(summary = "List all unarchived subjects the student is enrolled in")
    public ResponseEntity<List<SubjectsSummaryStudentResponse>> listStudentSubjects(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(subjectService.listStudentSubjects(principal.getId()));
    }

    @GetMapping("/{id}/student")
    @Operation(summary = "Get subject details for the student")
    public ResponseEntity<SubjectDetailsStudentResponse> getStudentSubjectDetails(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        return ResponseEntity.ok(subjectService.getStudentSubjectDetails(subjectId, principal.getId()));
    }

    @GetMapping("/{id}/teacher/profile")
    @Operation(summary = "Get the profile of this subject's teacher")
    public ResponseEntity<TeacherProfileResponse> getTeacherProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        return ResponseEntity.ok(subjectService.getTeacherProfile(subjectId, principal.getId()));
    }

    @PostMapping("/join")
    @Operation(summary = "Enroll in a subject using a 6-character join code")
    public ResponseEntity<SubjectsSummaryStudentResponse> joinSubject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SubjectJoinRequest request
    ) {
        return ResponseEntity.ok(subjectService.joinSubject(request, principal.getId()));
    }

    // --- Misc ---
    @GetMapping("/{id}/updated-at")
    @Operation(summary = "Get the latest update date of subject")
    public ResponseEntity<SubjectUpdatedAtResponse> getSubjectUpdatedAt(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long subjectId
    ) {
        return ResponseEntity.ok(subjectService.getSubjectUpdatedAt(subjectId, principal.getId()));
    }
}
