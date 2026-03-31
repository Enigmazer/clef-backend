package com.enigmazer.clef.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class E164PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private final PhoneNumberUtil util = PhoneNumberUtil.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        try {
            return util.isValidNumber(util.parse(value, null));
        } catch (NumberParseException e) {
            return false;
        }
    }
}