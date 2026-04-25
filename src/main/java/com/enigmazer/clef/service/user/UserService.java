package com.enigmazer.clef.service.user;

import com.enigmazer.clef.dto.user.AvatarUploadResponse;
import com.enigmazer.clef.dto.user.UserPreferenceUpdateResponse;
import com.enigmazer.clef.dto.user.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserResponse getCurrentUser(Long userId);

    UserPreferenceUpdateResponse toggleUserPhoneVisibility(Long userId);

    UserPreferenceUpdateResponse toggleUserStudentSectionVisibility(Long userId);

    UserPreferenceUpdateResponse toggleUserTeacherSectionVisibility(Long userId);

    AvatarUploadResponse uploadAvatar(Long userId, MultipartFile file);

    void deleteUserAvatar(Long userId);
}
