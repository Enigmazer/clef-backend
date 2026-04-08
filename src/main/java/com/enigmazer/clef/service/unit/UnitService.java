package com.enigmazer.clef.service.unit;

import com.enigmazer.clef.dto.unit.UnitUpdateRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateResponse;

public interface UnitService {
    UnitUpdateResponse updateUnit(Long subjectId, Long unitId, UnitUpdateRequest request, Long teacherId);

    void deleteUnit(Long subjectId, Long unitId, Long teacherId);
}
