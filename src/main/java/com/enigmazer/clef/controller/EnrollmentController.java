package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.enrollment.EnrolledStudentResponse;
import com.enigmazer.clef.service.enrollment.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("subjects/{subjectId}/enrollments")
@Tag(name = "Enrollment Management")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @Operation(summary = "List all students enrolled in the subject")
    public ResponseEntity<List<EnrolledStudentResponse>> listEnrolledStudents(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId
    ) {
        return ResponseEntity.ok(enrollmentService.listEnrolledStudents(subjectId, principal.getId()));
    }

    @DeleteMapping("/{studentId}")
    @Operation(summary = "Remove a student's enrollment from the subject")
    public ResponseEntity<Void> removeEnrollment(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long studentId
    ) {
        enrollmentService.removeEnrollment(subjectId, studentId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Remove your enrollment from the subject")
    public ResponseEntity<Void> removeYourEnrollment(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId
    ) {
        enrollmentService.unEnrollment(subjectId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
