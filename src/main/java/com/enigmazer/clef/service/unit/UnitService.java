package com.enigmazer.clef.service.unit;

import com.enigmazer.clef.dto.unit.UnitCreationRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateResponse;

import java.util.List;

public interface UnitService {
    void bulkAddUnits(Long subjectId, List<UnitCreationRequest> request, Long teacherId);

    UnitUpdateResponse updateUnit(Long subjectId, Long unitId, UnitUpdateRequest request, Long teacherId);

    void deleteUnit(Long subjectId, Long unitId, Long teacherId);
}
