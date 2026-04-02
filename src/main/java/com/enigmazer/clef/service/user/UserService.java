package com.enigmazer.clef.service.user;

import com.enigmazer.clef.dto.user.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(Long userId);

    boolean toggleUserPhoneVisibility(Long userId);

    boolean toggleUserStudentSectionVisibility(Long userId);

    boolean toggleUserTeacherSectionVisibility(Long userId);
}
