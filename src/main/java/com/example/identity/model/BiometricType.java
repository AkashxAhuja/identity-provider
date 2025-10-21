package com.example.identity.model;

public enum BiometricType {
    NONE('N'),
    FINGERPRINT('F'),
    FACE('C');

    private final char code;

    BiometricType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static BiometricType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return NONE;
        }
        char normalized = Character.toUpperCase(code.charAt(0));
        for (BiometricType type : values()) {
            if (type.code == normalized) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported biometric type code: " + code);
    }
}
