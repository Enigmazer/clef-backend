package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.unit.UnitDetailResponse;
import com.enigmazer.clef.dto.unit.UnitUpdateResponse;
import com.enigmazer.clef.entity.Unit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {SimpleTopicMapper.class})
public interface UnitMapper {

    UnitDetailResponse toDetailResponse(Unit unit);

    UnitUpdateResponse toUpdateResponse(Unit unit);
}
