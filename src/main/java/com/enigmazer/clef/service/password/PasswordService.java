package com.enigmazer.clef.service.password;

import com.enigmazer.clef.dto.auth.SetPasswordRequest;
import com.enigmazer.clef.dto.auth.UpdatePasswordRequest;

public interface PasswordService {
    void setPassword(SetPasswordRequest request, Long userId);

    void updatePassword(UpdatePasswordRequest request, Long userId);
}
