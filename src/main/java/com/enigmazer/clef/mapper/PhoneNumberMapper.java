package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.phone.PhoneResponse;
import com.enigmazer.clef.entity.PhoneNumber;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PhoneNumberMapper {
    @Mapping(source = "primary", target = "primary")
    PhoneResponse toResponse(PhoneNumber phoneNumber);
}
