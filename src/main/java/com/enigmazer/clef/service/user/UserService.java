package com.enigmazer.clef.service.user;

import com.enigmazer.clef.dto.user.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserResponse getCurrentUser(Long userId);

    boolean toggleUserPhoneVisibility(Long userId);

    boolean toggleUserStudentSectionVisibility(Long userId);

    boolean toggleUserTeacherSectionVisibility(Long userId);

    String uploadAvatar(Long userId, MultipartFile file);

    void deleteUserAvatar(Long userId);
}
