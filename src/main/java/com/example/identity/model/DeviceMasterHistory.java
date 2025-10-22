package com.example.identity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "device_master_history")
public class DeviceMasterHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "device_id", nullable = false, length = 200)
    private String deviceId;

    @Column(name = "cid", nullable = false, length = 20)
    private String cid;

    @Column(name = "device_sec_key", length = 500)
    private String deviceSecKey;

    @Column(name = "is_touch_enabled", nullable = false, length = 1)
    private String touchEnabledFlag;

    @Column(name = "status", nullable = false, length = 1)
    private String status;

    @Column(name = "device_os", nullable = false, length = 20)
    private String deviceOs;

    @Column(name = "device_os_ver", nullable = false, length = 20)
    private String deviceOsVer;

    @Column(name = "device_model", nullable = false, length = 100)
    private String deviceModel;

    @Column(name = "biometric_type_flg", nullable = false, length = 1)
    private String biometricTypeFlag;

    @Column(name = "ime_no", length = 100)
    private String imeNo;

    @Column(name = "device_nick_name", length = 100)
    private String deviceNickName;

    @Column(name = "reg_date", nullable = false)
    private OffsetDateTime regDate;

    @Column(name = "last_update_datetime", nullable = false)
    private OffsetDateTime lastUpdateDateTime;

    @Column(name = "app_version", nullable = false, length = 15)
    private String appVersion;

    @Column(name = "lang", length = 5)
    private String lang;

    @Column(name = "device_os_id", length = 100)
    private String deviceOsId;

    @Column(name = "detag_reason", length = 255)
    private String detagReason;

    @Column(name = "detag_datetime", nullable = false)
    private OffsetDateTime detagDateTime;

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getDeviceSecKey() {
        return deviceSecKey;
    }

    public void setDeviceSecKey(String deviceSecKey) {
        this.deviceSecKey = deviceSecKey;
    }

    public String getTouchEnabledFlag() {
        return touchEnabledFlag;
    }

    public void setTouchEnabledFlag(String touchEnabledFlag) {
        this.touchEnabledFlag = touchEnabledFlag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getBiometricTypeFlag() {
        return biometricTypeFlag;
    }

    public void setBiometricTypeFlag(String biometricTypeFlag) {
        this.biometricTypeFlag = biometricTypeFlag;
    }

    public String getImeNo() {
        return imeNo;
    }

    public void setImeNo(String imeNo) {
        this.imeNo = imeNo;
    }

    public String getDeviceNickName() {
        return deviceNickName;
    }

    public void setDeviceNickName(String deviceNickName) {
        this.deviceNickName = deviceNickName;
    }

    public OffsetDateTime getRegDate() {
        return regDate;
    }

    public void setRegDate(OffsetDateTime regDate) {
        this.regDate = regDate;
    }

    public OffsetDateTime getLastUpdateDateTime() {
        return lastUpdateDateTime;
    }

    public void setLastUpdateDateTime(OffsetDateTime lastUpdateDateTime) {
        this.lastUpdateDateTime = lastUpdateDateTime;
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

    public String getDeviceOsId() {
        return deviceOsId;
    }

    public void setDeviceOsId(String deviceOsId) {
        this.deviceOsId = deviceOsId;
    }

    public String getDetagReason() {
        return detagReason;
    }

    public void setDetagReason(String detagReason) {
        this.detagReason = detagReason;
    }

    public OffsetDateTime getDetagDateTime() {
        return detagDateTime;
    }

    public void setDetagDateTime(OffsetDateTime detagDateTime) {
        this.detagDateTime = detagDateTime;
    }
}
