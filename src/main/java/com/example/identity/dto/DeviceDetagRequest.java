package com.example.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DeviceDetagRequest {

    @NotBlank
    private String cid;

    @NotBlank
    private String deviceId;

    private String reason;

    @NotNull
    private Boolean revokeSessions;

    @NotNull
    private Boolean hardDelete;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getRevokeSessions() {
        return revokeSessions;
    }

    public void setRevokeSessions(Boolean revokeSessions) {
        this.revokeSessions = revokeSessions;
    }

    public Boolean getHardDelete() {
        return hardDelete;
    }

    public void setHardDelete(Boolean hardDelete) {
        this.hardDelete = hardDelete;
    }
}
