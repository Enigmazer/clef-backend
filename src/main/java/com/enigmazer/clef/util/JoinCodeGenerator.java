package com.enigmazer.clef.util;

import java.security.SecureRandom;

public class JoinCodeGenerator {

    //removed 0, O, 1, I and L to prevent typos
    private static final String ALLOWED_CHARACTERS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private JoinCodeGenerator(){
        throw new UnsupportedOperationException("Utility class");
    }

    public static String generate(){
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for(int i=0; i<CODE_LENGTH; i++){
            int randomIndex = RANDOM.nextInt(ALLOWED_CHARACTERS.length());
            code.append(ALLOWED_CHARACTERS.charAt(randomIndex));
        }
        return code.toString();
    }
}
