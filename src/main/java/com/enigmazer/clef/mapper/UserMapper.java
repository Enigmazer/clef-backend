package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.user.UserResponse;
import com.enigmazer.clef.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role" , expression = "java(user.getRole().getName().name())")
    @Mapping(target = "hasPassword", expression = "java(user.getPassword() != null && !user.getPassword().isEmpty())")
    @Mapping(source = "twoFactorEnabled", target = "twoFactorEnabled")
    @Mapping(source = "showPhoneToStudents", target = "showPhoneToStudents")
    UserResponse toResponse(User user);
}
