package com.example.identity.util;

import com.example.identity.dto.DeviceResponse;
import com.example.identity.model.DeviceMaster;

public final class DeviceMapper {

    private DeviceMapper() {
    }

    public static DeviceResponse toResponse(DeviceMaster entity) {
        DeviceResponse response = new DeviceResponse();
        response.setDeviceId(entity.getDeviceId());
        response.setCid(entity.getCid());
        response.setDeviceOs(entity.getDeviceOs());
        response.setDeviceOsVer(entity.getDeviceOsVer());
        response.setModel(entity.getDeviceModel());
        response.setAppVersion(entity.getAppVersion());
        response.setDeviceNickName(entity.getDeviceNickName());
        response.setStatus(entity.getStatus());
        response.setTouchEnabled(entity.isTouchEnabled());
        response.setBiometricType(entity.getBiometricType().name());
        response.setRegDate(entity.getRegDate());
        response.setLastUpdateDateTime(entity.getLastUpdateDateTime());
        return response;
    }
}
