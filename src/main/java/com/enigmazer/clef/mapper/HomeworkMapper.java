package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.homework.HomeworkResponse;
import com.enigmazer.clef.entity.Homework;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HomeworkMapper {

    HomeworkResponse toResponse(Homework homework);
}
