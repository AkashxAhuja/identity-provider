package com.example.identity.service;

import com.example.identity.dto.BiometricRegistrationRequest;
import com.example.identity.dto.DeviceDetagRequest;
import com.example.identity.dto.DeviceRegistrationRequest;
import com.example.identity.dto.DeviceResponse;
import com.example.identity.exception.ConflictException;
import com.example.identity.exception.ResourceNotFoundException;
import com.example.identity.model.BiometricType;
import com.example.identity.model.DeviceMaster;
import com.example.identity.model.DeviceMasterHistory;
import com.example.identity.model.IdempotencyRecord;
import com.example.identity.repository.DeviceMasterHistoryRepository;
import com.example.identity.repository.DeviceMasterRepository;
import com.example.identity.repository.IdempotencyRecordRepository;
import com.example.identity.util.DeviceMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceMasterRepository deviceMasterRepository;
    private final DeviceMasterHistoryRepository deviceMasterHistoryRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public DeviceService(DeviceMasterRepository deviceMasterRepository,
                         DeviceMasterHistoryRepository deviceMasterHistoryRepository,
                         IdempotencyRecordRepository idempotencyRecordRepository,
                         ObjectMapper objectMapper) {
        this.deviceMasterRepository = deviceMasterRepository;
        this.deviceMasterHistoryRepository = deviceMasterHistoryRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeviceRegistrationResult registerDevice(DeviceRegistrationRequest request, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = hashRequest(request);

        if (normalizedKey != null) {
            Optional<IdempotencyRecord> cached = idempotencyRecordRepository
                    .findByIdempotencyKeyAndRequestHash(normalizedKey, requestHash);
            if (cached.isPresent()) {
                DeviceResponse cachedResponse = deserializeResponse(cached.get().getResponsePayload());
                return new DeviceRegistrationResult(cachedResponse, false);
            }
        }

        Optional<DeviceMaster> fingerprintMatch = findExistingDevice(request);
        if (fingerprintMatch.isPresent()) {
            DeviceResponse response = DeviceMapper.toResponse(fingerprintMatch.get());
            return new DeviceRegistrationResult(response, false);
        }

        DeviceMaster entity = DeviceMaster.newDevice(request.getCid());
        entity.setDeviceOs(request.getDeviceOs().toUpperCase());
        entity.setDeviceOsVer(request.getDeviceOsVer());
        entity.setDeviceModel(request.getModel());
        entity.setAppVersion(request.getAppVersion());
        entity.setLang(request.getLang());
        entity.setImeNo(request.getImeNo());
        entity.setDeviceNickName(request.getDeviceNickName());
        entity.setTouchEnabled(Boolean.TRUE.equals(request.getTouchEnabled()));
        entity.setBiometricType(BiometricType.valueOf(request.getBiometricType().toUpperCase()));
        entity.setDeviceOsId(request.getDeviceOsId());
        entity.setLastUpdateDateTime(OffsetDateTime.now(ZoneOffset.UTC));

        if (request.getAttestationProof() != null) {
            entity.setDeviceSecKey(hashSecret(request.getAttestationProof()));
        }

        DeviceMaster saved = deviceMasterRepository.save(entity);
        DeviceResponse response = DeviceMapper.toResponse(saved);

        if (normalizedKey != null) {
            persistIdempotencyResponse(normalizedKey, requestHash, response);
        }
        return new DeviceRegistrationResult(response, true);
    }

    @Transactional
    public DeviceResponse enableBiometrics(BiometricRegistrationRequest request) {
        DeviceMaster device = deviceMasterRepository.findById(request.getDeviceId())
                .filter(existing -> existing.getCid().equals(request.getCid()))
                .orElseThrow(() -> new ResourceNotFoundException("Device not found for customer"));

        BiometricType requestedType = BiometricType.valueOf(request.getBiometricType().toUpperCase());
        BiometricType currentType = device.getBiometricType();
        if (currentType != BiometricType.NONE && currentType != requestedType) {
            throw new ConflictException("Device already registered with a different biometric type");
        }

        device.setBiometricType(requestedType);
        device.setTouchEnabled(Boolean.TRUE.equals(request.getTouchEnabled()));
        device.setLastUpdateDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        if (request.getAttestationProof() != null) {
            device.setDeviceSecKey(hashSecret(request.getAttestationProof()));
        }

        DeviceMaster updated = deviceMasterRepository.save(device);
        return DeviceMapper.toResponse(updated);
    }

    @Transactional
    public DeviceResponse detagDevice(DeviceDetagRequest request) {
        DeviceMaster device = deviceMasterRepository.findById(request.getDeviceId())
                .filter(existing -> existing.getCid().equals(request.getCid()))
                .orElseThrow(() -> new ResourceNotFoundException("Device not found for customer"));

        DeviceMasterHistory history = new DeviceMasterHistory();
        history.setDeviceId(device.getDeviceId());
        history.setCid(device.getCid());
        history.setDeviceSecKey(device.getDeviceSecKey());
        history.setTouchEnabledFlag(device.getTouchEnabledFlag());
        history.setStatus("I");
        history.setDeviceOs(device.getDeviceOs());
        history.setDeviceOsVer(device.getDeviceOsVer());
        history.setDeviceModel(device.getDeviceModel());
        history.setBiometricTypeFlag(device.getBiometricTypeFlag());
        history.setImeNo(device.getImeNo());
        history.setDeviceNickName(device.getDeviceNickName());
        history.setRegDate(device.getRegDate());
        history.setLastUpdateDateTime(device.getLastUpdateDateTime());
        history.setAppVersion(device.getAppVersion());
        history.setLang(device.getLang());
        history.setDeviceOsId(device.getDeviceOsId());
        history.setDetagReason(request.getReason());
        history.setDetagDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        deviceMasterHistoryRepository.save(history);

        device.setStatus("I");
        device.setSessionId(null);
        device.setPreLoginDashboardKey(null);
        device.setLastUpdateDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        device.setTouchEnabled(false);
        device.setBiometricType(BiometricType.NONE);

        DeviceResponse response = DeviceMapper.toResponse(device);

        if (Boolean.TRUE.equals(request.getHardDelete())) {
            deviceMasterRepository.delete(device);
        } else {
            deviceMasterRepository.save(device);
        }

        if (Boolean.TRUE.equals(request.getRevokeSessions())) {
            log.info("DeviceDetagged event published for device {} and customer {}", device.getDeviceId(), device.getCid());
        }

        return response;
    }

    private Optional<DeviceMaster> findExistingDevice(DeviceRegistrationRequest request) {
        if (request.getImeNo() != null && !request.getImeNo().isBlank()) {
            Optional<DeviceMaster> byImei = deviceMasterRepository.findFirstByCidAndImeNo(request.getCid(), request.getImeNo());
            if (byImei.isPresent()) {
                return byImei;
            }
        }
        if (request.getDeviceOsId() != null && !request.getDeviceOsId().isBlank()) {
            Optional<DeviceMaster> byOsId = deviceMasterRepository.findFirstByCidAndDeviceOsId(request.getCid(), request.getDeviceOsId());
            if (byOsId.isPresent()) {
                return byOsId;
            }
        }
        return deviceMasterRepository.findFirstByCidAndDeviceModelAndDeviceOsVer(request.getCid(), request.getModel(), request.getDeviceOsVer());
    }

    private String hashRequest(DeviceRegistrationRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            return DigestUtils.md5DigestAsHex(payload.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize request", e);
        }
    }

    private void persistIdempotencyResponse(String key, String requestHash, DeviceResponse response) {
        try {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setIdempotencyKey(key);
            record.setRequestHash(requestHash);
            record.setResponsePayload(objectMapper.writeValueAsString(response));
            record.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            idempotencyRecordRepository.save(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to persist idempotent response", e);
        }
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.trim();
    }

    private DeviceResponse deserializeResponse(String payload) {
        try {
            return objectMapper.readValue(payload, DeviceResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to read cached response", e);
        }
    }

    private String hashSecret(String secret) {
        return DigestUtils.md5DigestAsHex(secret.getBytes(StandardCharsets.UTF_8));
    }
}
