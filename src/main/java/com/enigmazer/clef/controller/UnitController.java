package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.unit.UnitCreationRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateResponse;
import com.enigmazer.clef.service.unit.UnitService;
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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("subjects/{subjectId}/units")
@Tag(name = "Unit Management")
public class UnitController {

    private final UnitService unitService;

    // --- Create Operations ---
    @PostMapping("/bulk")
    @Operation(summary = "Bulk add units and their topics to the subject")
    public ResponseEntity<Void> bulkAddUnits(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @NotEmpty(message = "At least one unit is required.")
            @Valid @RequestBody List<UnitCreationRequest> request
    ) {
        unitService.bulkAddUnits(subjectId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // --- Patch Operations ---
    @PatchMapping("/{unitId}")
    @Operation(summary = "Update the title of the unit and add new topics in it.")
    public ResponseEntity<UnitUpdateResponse> updateUnit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId,
            @Valid @RequestBody UnitUpdateRequest request
    ){
        return ResponseEntity.ok()
                .body(unitService.updateUnit(subjectId, unitId, request, principal.getId()));
    }

    // --- Delete Operations ---
    @DeleteMapping("/{unitId}")
    @Operation(summary = "Delete a unit.")
    public ResponseEntity<Void> deleteUnit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long subjectId,
            @PathVariable Long unitId
    ){
        unitService.deleteUnit(subjectId, unitId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
