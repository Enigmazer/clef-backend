package com.enigmazer.clef.service.phone;

import com.enigmazer.clef.dto.phone.PhoneResponse;

import java.util.List;

public interface PhoneNumberService {

    void sendOtp(String rawPhone, Long userId);

    void verifyOtp(Long userId, String rawPhone, String code);

    List<PhoneResponse> getUserPhoneNumbers(Long userId);

    void setPrimaryPhoneNumber(Long userId);

    void deleteUserPhoneNumber(String phoneNumber, Long userId);

    void send2FAOtp(Long userId);

    void verify2FAOtp(Long userId, String code);
}
