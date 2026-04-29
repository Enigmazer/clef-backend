package com.enigmazer.clef.service.password;

import com.enigmazer.clef.dto.auth.SetPasswordRequest;
import com.enigmazer.clef.dto.auth.UpdatePasswordRequest;
import com.enigmazer.clef.entity.User;
import com.enigmazer.clef.exception.BusinessException;
import com.enigmazer.clef.exception.SystemResourceNotFoundException;
import com.enigmazer.clef.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordServiceImpl implements PasswordService{

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void setPassword(SetPasswordRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found", userId));

        if(user.getPassword() != null){
            throw new BusinessException("You already have a password set");
        }

        user.setPassword(passwordEncoder.encode(request.password()));
        log.info("Successfully set password [userId={}]", userId);
    }

    @Override
    @Transactional
    public void updatePassword(UpdatePasswordRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found", userId));

        if(!passwordEncoder.matches(request.currentPassword(), user.getPassword())){
            throw new BusinessException("Current password is incorrect");
        }

        if(passwordEncoder.matches(request.newPassword(), user.getPassword())){
            throw new BusinessException("New password must be different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        log.info("Successfully updated password [userId={}]", userId);
    }
}
