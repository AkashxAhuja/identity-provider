package com.example.identity.service;

import com.example.identity.dto.DeviceResponse;

public class DeviceRegistrationResult {

    private final DeviceResponse deviceResponse;
    private final boolean created;

    public DeviceRegistrationResult(DeviceResponse deviceResponse, boolean created) {
        this.deviceResponse = deviceResponse;
        this.created = created;
    }

    public DeviceResponse getDeviceResponse() {
        return deviceResponse;
    }

    public boolean isCreated() {
        return created;
    }
}
