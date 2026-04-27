package com.enigmazer.clef.service.user;

import com.enigmazer.clef.dto.user.AvatarUploadResponse;
import com.enigmazer.clef.dto.user.UserPreferenceUpdateResponse;
import com.enigmazer.clef.dto.user.UserResponse;
import com.enigmazer.clef.entity.User;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.SystemResourceNotFoundException;
import com.enigmazer.clef.mapper.UserMapper;
import com.enigmazer.clef.repository.UserRepository;
import com.enigmazer.clef.service.storage.StorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    private final StorageService storageService;

    private final UserMapper userMapper;

    @Override
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new SystemResourceNotFoundException("User not found", userId)
        );

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserPreferenceUpdateResponse toggleUserPhoneVisibility(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found", userId));

        boolean newValue = !user.isShowPhoneToStudents();
        user.setShowPhoneToStudents(newValue);
        log.info("Toggled user's phone number visibility [newValue={}, userId={}]", newValue, userId);
        return userMapper.toPreferenceUpdateResponse(user);
    }

    @Override
    @Transactional
    public UserPreferenceUpdateResponse toggleUserStudentSectionVisibility(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found", userId));

        if(user.isHideTeacherSection()){
            throw new InvalidRequestException("You can not hide both sections");
        }

        boolean newValue = !user.isHideStudentSection();
        user.setHideStudentSection(newValue);
        log.info("Toggled user's student section visibility [newValue={}, userId={}]", newValue, userId);
        return userMapper.toPreferenceUpdateResponse(user);
    }

    @Override
    @Transactional
    public UserPreferenceUpdateResponse toggleUserTeacherSectionVisibility(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found", userId));


        if(user.isHideStudentSection()){
            throw new InvalidRequestException("You can not hide both sections");
        }

        boolean newValue = !user.isHideTeacherSection();
        user.setHideTeacherSection(newValue);
        log.info("Toggled user's teacher section visibility [newValue={}, userId={}]", newValue, userId);
        return userMapper.toPreferenceUpdateResponse(user);
    }

    @Override
    @Transactional
    public AvatarUploadResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found", userId));

        if (user.getAvatarUrl() != null){
            deleteAvatar(user.getAvatarUrl());
        }

        String avatarKey = storageService.generateAvatarKey(file);
        String avatarUrl = storageService.generateAvatarUrl(avatarKey);
        user.setAvatarUrl(avatarUrl);

        storageService.uploadAvatar(file, avatarKey);

        log.info("Uploaded new avatar for user [userId={}]",userId);
        return new AvatarUploadResponse(avatarUrl);
    }

    @Override
    @Transactional
    public void deleteUserAvatar(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found", userId));

        if (user.getAvatarUrl() != null){
            deleteAvatar(user.getAvatarUrl());
            user.setAvatarUrl(null);
        }
        log.info("deleted avatar for user [userId={}]",userId);
    }

    // --- Helper Methods ---
    private void deleteAvatar(String avatarUrl){
        storageService.deleteAvatar(avatarUrl);
    }
}
