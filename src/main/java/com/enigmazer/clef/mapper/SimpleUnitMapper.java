package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.unit.UnitResponse;
import com.enigmazer.clef.entity.Unit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SimpleUnitMapper {
    // Maps a Unit without its Topics
    UnitResponse toResponse(Unit unit);
}