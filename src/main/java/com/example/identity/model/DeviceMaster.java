package com.example.identity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "device_master")
public class DeviceMaster {

    @Id
    @Column(name = "device_id", nullable = false, length = 200)
    private String deviceId;

    @Column(name = "cid", nullable = false, length = 20)
    private String cid;

    @Column(name = "device_sec_key", length = 500)
    private String deviceSecKey;

    @Column(name = "is_touch_enabled", nullable = false, length = 1)
    private String touchEnabledFlag;

    @Column(name = "is_migrated", length = 1)
    private String migratedFlag = "N";

    @Column(name = "status", nullable = false, length = 1)
    private String status;

    @Column(name = "audit_id", length = 20)
    private String auditId;

    @Column(name = "device_os", nullable = false, length = 20)
    private String deviceOs;

    @Column(name = "device_os_ver", nullable = false, length = 20)
    private String deviceOsVer;

    @Column(name = "device_model", nullable = false, length = 100)
    private String deviceModel;

    @Column(name = "pre_dshboard_enabled", length = 1)
    private String preDashboardEnabled;

    @Column(name = "biometric_type_flg", nullable = false, length = 1)
    private String biometricTypeFlag;

    @Column(name = "pre_lgn_dshboard_key", length = 500)
    private String preLoginDashboardKey;

    @Column(name = "session_id", length = 200)
    private String sessionId;

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

    public DeviceMaster() {
    }

    public static DeviceMaster newDevice(String cid) {
        DeviceMaster device = new DeviceMaster();
        device.deviceId = UUID.randomUUID().toString();
        device.cid = cid;
        device.status = "A";
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        device.regDate = now;
        device.lastUpdateDateTime = now;
        device.biometricTypeFlag = BiometricType.NONE.name().substring(0, 1);
        device.touchEnabledFlag = "N";
        return device;
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

    public String getMigratedFlag() {
        return migratedFlag;
    }

    public void setMigratedFlag(String migratedFlag) {
        this.migratedFlag = migratedFlag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
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

    public String getPreDashboardEnabled() {
        return preDashboardEnabled;
    }

    public void setPreDashboardEnabled(String preDashboardEnabled) {
        this.preDashboardEnabled = preDashboardEnabled;
    }

    public String getBiometricTypeFlag() {
        return biometricTypeFlag;
    }

    public void setBiometricTypeFlag(String biometricTypeFlag) {
        this.biometricTypeFlag = biometricTypeFlag;
    }

    public String getPreLoginDashboardKey() {
        return preLoginDashboardKey;
    }

    public void setPreLoginDashboardKey(String preLoginDashboardKey) {
        this.preLoginDashboardKey = preLoginDashboardKey;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    @Transient
    public boolean isTouchEnabled() {
        return Objects.equals(touchEnabledFlag, "Y");
    }

    @Transient
    public void setTouchEnabled(boolean enabled) {
        this.touchEnabledFlag = enabled ? "Y" : "N";
    }

    @Transient
    public BiometricType getBiometricType() {
        return BiometricType.fromCode(biometricTypeFlag);
    }

    @Transient
    public void setBiometricType(BiometricType type) {
        this.biometricTypeFlag = String.valueOf(type.getCode());
    }
}
