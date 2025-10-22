package com.example.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class DeviceRegistrationRequest {

    @NotBlank
    private String cid;

    @NotBlank
    private String deviceOs;

    @NotBlank
    private String deviceOsVer;

    @NotBlank
    private String model;

    @NotBlank
    private String appVersion;

    @NotBlank
    private String lang;

    private String imeNo;

    private String deviceOsId;

    @NotBlank
    private String userAgent;

    @NotNull
    private Boolean isTouchEnabled;

    @NotBlank
    @Pattern(regexp = "NONE|FINGERPRINT|FACE", message = "Invalid biometric type")
    private String biometricType;

    private String deviceNickName;

    private String attestationProof;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getDeviceOs() {
        return deviceOs;
    }

    public void setDeviceOs(String deviceOs) {
        this.deviceOs = deviceOs;
    }

    public String getDeviceOsVer() {
        return deviceOsVer;
    }

    public void setDeviceOsVer(String deviceOsVer) {
        this.deviceOsVer = deviceOsVer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getImeNo() {
        return imeNo;
    }

    public void setImeNo(String imeNo) {
        this.imeNo = imeNo;
    }

    public String getDeviceOsId() {
        return deviceOsId;
    }

    public void setDeviceOsId(String deviceOsId) {
        this.deviceOsId = deviceOsId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Boolean getTouchEnabled() {
        return isTouchEnabled;
    }

    public void setTouchEnabled(Boolean touchEnabled) {
        isTouchEnabled = touchEnabled;
    }

    public String getBiometricType() {
        return biometricType;
    }

    public void setBiometricType(String biometricType) {
        this.biometricType = biometricType;
    }

    public String getDeviceNickName() {
        return deviceNickName;
    }

    public void setDeviceNickName(String deviceNickName) {
        this.deviceNickName = deviceNickName;
    }

    public String getAttestationProof() {
        return attestationProof;
    }

    public void setAttestationProof(String attestationProof) {
        this.attestationProof = attestationProof;
    }
}
