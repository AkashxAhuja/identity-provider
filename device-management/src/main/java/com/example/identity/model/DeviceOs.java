package com.example.identity.model;

public enum DeviceOs {
    ANDROID,
    IOS;

    public static DeviceOs from(String value) {
        for (DeviceOs os : values()) {
            if (os.name().equalsIgnoreCase(value)) {
                return os;
            }
        }
        throw new IllegalArgumentException("Unsupported device OS: " + value);
    }
}
