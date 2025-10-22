package com.example.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class BiometricRegistrationRequest {

    @NotBlank
    private String cid;

    @NotBlank
    private String deviceId;

    @NotBlank
    @Pattern(regexp = "FINGERPRINT|FACE", message = "Biometric type must be FINGERPRINT or FACE")
    private String biometricType;

    @NotNull
    private Boolean isTouchEnabled;

    private String attestationProof;

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

    public String getBiometricType() {
        return biometricType;
    }

    public void setBiometricType(String biometricType) {
        this.biometricType = biometricType;
    }

    public Boolean getTouchEnabled() {
        return isTouchEnabled;
    }

    public void setTouchEnabled(Boolean touchEnabled) {
        isTouchEnabled = touchEnabled;
    }

    public String getAttestationProof() {
        return attestationProof;
    }

    public void setAttestationProof(String attestationProof) {
        this.attestationProof = attestationProof;
    }
}
