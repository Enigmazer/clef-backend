package com.enigmazer.clef.service.phone;

import com.enigmazer.clef.dto.phone.PhoneResponse;
import com.enigmazer.clef.entity.PhoneNumber;
import com.enigmazer.clef.entity.User;
import com.enigmazer.clef.exception.*;
import com.enigmazer.clef.mapper.PhoneNumberMapper;
import com.enigmazer.clef.repository.PhoneNumberRepository;
import com.enigmazer.clef.repository.UserRepository;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhoneNumberServiceImpl implements PhoneNumberService{

    @Value("${twilio.verify.service-sid}")
    private String serviceSid;

    private final PhoneNumberRepository phoneNumberRepository;
    private final UserRepository userRepository;

    private final PhoneNumberMapper phoneNumberMapper;

    // real otp verification is disabled because of free tire limitations
    @Override
    public void sendOtp(String rawPhone, Long userId) {
        if (phoneNumberRepository.countByUserId(userId) >= 2) {
            throw new BusinessException("Maximum of 2 phone numbers allowed");
        }

        String normalized = normalize(rawPhone);

        if (phoneNumberRepository.existsByPhoneNumberAndUserId(normalized, userId)) {
            log.warn("User attempted to register already owned number [userId={}]", userId);
            throw new ResourceAlreadyExistsException("Phone number is already registered.");
        }

        if (phoneNumberRepository.existsByPhoneNumber(normalized)) {
            log.warn("User attempted to register already taken phone number [userId={}]", userId);
            throw new BusinessException("Phone number is unavailable.");
        }

//        Verification.creator(serviceSid, normalized, "sms").create();
        log.info("otp sent [userId={}]", userId);
    }

    @Override
    @Transactional
    public void verifyOtp(Long userId, String rawPhone, String code) {
        String normalized = normalize(rawPhone);

        fakeOtpVerification(userId, normalized, code);
//        otpVerification(userId, normalized, code);

        boolean isPrimaryPhone = phoneNumberRepository.countByUserId(userId) == 0;

        User user = userRepository.getReferenceById(userId);

        PhoneNumber phoneNumber = PhoneNumber.builder()
                .user(user)
                .phoneNumber(normalized)
                .isPrimary(isPrimaryPhone)
                .build();

        phoneNumberRepository.save(phoneNumber);
        log.info("Successfully verified and saved phone number [userId={}]", userId);
    }

    @Override
    public List<PhoneResponse> getUserPhoneNumbers(Long userId) {
        return phoneNumberRepository.findAllByUserId(userId)
                .stream()
                .map(phoneNumberMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void setPrimaryPhoneNumber(Long userId) {
        if(is2FAEnabled(userId)){
            log.warn("User attempted to change primary number with 2FA enabled [userId={}]", userId);
            throw new BusinessException("Please disable 2FA before changing your primary number.");
        }

        PhoneNumber target = phoneNumberRepository.findByUserIdAndIsPrimaryFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone number not found."));

        phoneNumberRepository.clearPrimaryByUserId(userId);
        target.setPrimary(true);
        log.info("User changed primary phone number [userId={}]", userId);
    }

    @Override
    @Transactional
    public void deleteUserPhoneNumber(String rawPhone, Long userId) {
        String normalized = normalize(rawPhone);

        PhoneNumber target = phoneNumberRepository.findByPhoneNumberAndUserId(normalized, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone number not found."));

        if (target.isPrimary()) {
            if(is2FAEnabled(userId)){
                log.warn("User attempted to delete primary number with 2FA enabled [userId={}]", userId);
                throw new BusinessException("Please disable 2FA before deleting your primary number.");
            }

            phoneNumberRepository.findByUserIdAndIsPrimaryFalse(userId)
                    .ifPresent(remaining -> {
                        remaining.setPrimary(true);
                        log.info("Auto-promoted user's remaining number to primary [userId={}]", userId);
                    });
        }

        phoneNumberRepository.delete(target);
        log.info("Successfully deleted phone number [userId={}]", userId);
    }

    @Override
    public void send2FAOtp(Long userId){
        String number = getPrimaryPhoneNumber(userId);

//        Verification.creator(serviceSid, number, "sms").create();
        log.info("2FA otp sent [userId={}]", userId);
    }

    @Override
    public void verify2FAOtp(Long userId, String code){
        String number = getPrimaryPhoneNumber(userId);

        fakeOtpVerification(userId, number, code);
//        otpVerification(userId, number, code);
        log.info("2FA otp verified [userId={}]", userId);
    }


    // --- Helper Methods ---
    private void fakeOtpVerification(Long userId, String number, String code){
        String otp = number.replaceAll("\\D", "");
        otp = otp.substring(Math.max(0, otp.length() - 6));
        if (!otp.equals(code)) {
            log.warn("Invalid OTP attempt [userId={}]", userId);
            throw new BusinessException("Invalid OTP");
        }
    }

    private void otpVerification(Long userId, String number, String code){
        VerificationCheck check = VerificationCheck.creator(serviceSid)
                .setTo(number)
                .setCode(code)
                .create();

        if (!"approved".equals(check.getStatus())) {
            log.warn("Invalid OTP attempt [userId={}]", userId);
            throw new BusinessException("Invalid or expired OTP");
        }
    }

    private boolean is2FAEnabled(Long userId){
        return userRepository.findById(userId).orElseThrow(
                () -> new SystemResourceNotFoundException("User not found", userId)
        ).isTwoFactorEnabled();
    }

    private String getPrimaryPhoneNumber(Long userId){
        PhoneNumber phoneNumber = phoneNumberRepository.findByUserIdAndIsPrimaryTrue(userId).orElseThrow(
                () -> new ResourceNotFoundException("No primary phone number found.")
        );

        return phoneNumber.getPhoneNumber();
    }

    private String normalize(String raw) {
        try {
            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
            return util.format(util.parse(raw, null), PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new InvalidRequestException("Invalid phone number");
        }
    }
}