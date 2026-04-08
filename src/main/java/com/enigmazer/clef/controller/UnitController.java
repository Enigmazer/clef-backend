package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.unit.UnitUpdateRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateResponse;
import com.enigmazer.clef.service.unit.UnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("subjects/{subjectId}/units")
@RequiredArgsConstructor
@Tag(name = "Unit Management")
public class UnitController {

    private final UnitService unitService;

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
