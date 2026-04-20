package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.phone.PhoneNumberStudentResponse;
import com.enigmazer.clef.dto.user.TeacherProfileResponse;
import com.enigmazer.clef.dto.user.UserResponse;
import com.enigmazer.clef.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PhoneNumberMapper.class})
public interface UserMapper {

    default List<PhoneNumberStudentResponse> mapPhoneNumbers(User user) {
        if (!user.isShowPhoneToStudents()) return null;
        return user.getPhoneNumbers().stream()
                .map(p -> new PhoneNumberStudentResponse(p.getPhoneNumber()))
                .toList();
    }

    @Mapping(target = "role" , expression = "java(user.getRole().getName().name())")
    @Mapping(target = "hasPassword", expression = "java(user.getPassword() != null && !user.getPassword().isEmpty())")
    @Mapping(source = "twoFactorEnabled", target = "twoFactorEnabled")
    @Mapping(source = "showPhoneToStudents", target = "showPhoneToStudents")
    UserResponse toResponse(User user);

    @Mapping(target = "phoneNumbers", expression = "java(mapPhoneNumbers(user))")
    TeacherProfileResponse toTeacherProfileResponse(User user);
}
