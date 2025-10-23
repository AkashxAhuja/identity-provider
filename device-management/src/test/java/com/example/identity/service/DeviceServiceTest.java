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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceMasterRepository deviceMasterRepository;
    @Mock
    private DeviceMasterHistoryRepository deviceMasterHistoryRepository;
    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;
    @Mock
    private ObjectMapper objectMapper;

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService(deviceMasterRepository, deviceMasterHistoryRepository, idempotencyRecordRepository, objectMapper);
    }

    @Test
    void registerDeviceReturnsCachedResponseWhenIdempotentMatchExists() throws Exception {
        DeviceRegistrationRequest request = buildRegistrationRequest();
        String serialized = "{\"cid\":\"123\"}";
        when(objectMapper.writeValueAsString(request)).thenReturn(serialized);
        String hash = DigestUtils.md5DigestAsHex(serialized.getBytes(StandardCharsets.UTF_8));
        DeviceResponse cachedResponse = new DeviceResponse();
        IdempotencyRecord record = new IdempotencyRecord();
        record.setResponsePayload("{\"deviceId\":\"cached\"}");
        when(idempotencyRecordRepository.findByIdempotencyKeyAndRequestHash("key", hash))
                .thenReturn(Optional.of(record));
        when(objectMapper.readValue(record.getResponsePayload(), DeviceResponse.class)).thenReturn(cachedResponse);

        DeviceRegistrationResult result = deviceService.registerDevice(request, " key ");

        assertThat(result.isCreated()).isFalse();
        assertThat(result.getDeviceResponse()).isEqualTo(cachedResponse);
        verify(idempotencyRecordRepository).findByIdempotencyKeyAndRequestHash("key", hash);
        verify(deviceMasterRepository, never()).save(any());
    }

    @Test
    void registerDeviceReturnsExistingFingerprintMatch() {
        DeviceService service = new DeviceService(deviceMasterRepository, deviceMasterHistoryRepository, idempotencyRecordRepository, new ObjectMapper());
        DeviceRegistrationRequest request = buildRegistrationRequest();
        DeviceMaster existing = prepareDeviceEntity(request.getCid());
        existing.setImeNo(request.getImeNo());
        when(idempotencyRecordRepository.findByIdempotencyKeyAndRequestHash(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndImeNo(request.getCid(), request.getImeNo()))
                .thenReturn(Optional.of(existing));

        DeviceRegistrationResult result = service.registerDevice(request, null);

        assertThat(result.isCreated()).isFalse();
        assertThat(result.getDeviceResponse().getDeviceId()).isEqualTo(existing.getDeviceId());
        verify(deviceMasterRepository, never()).save(any(DeviceMaster.class));
    }

    @Test
    void registerDeviceUsesDeviceOsIdLookupWhenImeiMissing() {
        DeviceService service = new DeviceService(deviceMasterRepository, deviceMasterHistoryRepository, idempotencyRecordRepository, new ObjectMapper());
        DeviceRegistrationRequest request = buildRegistrationRequest();
        request.setImeNo(null);
        DeviceMaster existing = prepareDeviceEntity(request.getCid());
        existing.setDeviceOsId(request.getDeviceOsId());
        when(idempotencyRecordRepository.findByIdempotencyKeyAndRequestHash(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndImeNo(request.getCid(), null)).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndDeviceOsId(request.getCid(), request.getDeviceOsId()))
                .thenReturn(Optional.of(existing));

        DeviceRegistrationResult result = service.registerDevice(request, null);

        assertThat(result.isCreated()).isFalse();
        assertThat(result.getDeviceResponse().getDeviceId()).isEqualTo(existing.getDeviceId());
    }

    @Test
    void registerDeviceCreatesNewRecordAndPersistsIdempotentResponse() throws Exception {
        DeviceRegistrationRequest request = buildRegistrationRequest();
        String serialized = "payload";
        when(objectMapper.writeValueAsString(isA(DeviceRegistrationRequest.class))).thenReturn(serialized);
        when(objectMapper.writeValueAsString(isA(DeviceResponse.class))).thenReturn("{\"deviceId\":\"new\"}");
        when(idempotencyRecordRepository.findByIdempotencyKeyAndRequestHash(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndImeNo(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndDeviceOsId(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndDeviceModelAndDeviceOsVer(any(), any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.save(any(DeviceMaster.class))).thenAnswer(invocation -> {
            DeviceMaster entity = invocation.getArgument(0);
            entity.setDeviceId("generated");
            return entity;
        });

        DeviceRegistrationResult result = deviceService.registerDevice(request, "idem");

        assertThat(result.isCreated()).isTrue();
        assertThat(result.getDeviceResponse().getDeviceId()).isEqualTo("generated");
        String hash = DigestUtils.md5DigestAsHex(serialized.getBytes(StandardCharsets.UTF_8));
        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("idem");
        assertThat(captor.getValue().getRequestHash()).isEqualTo(hash);
        assertThat(captor.getValue().getResponsePayload()).contains("new");
    }

    @Test
    void registerDeviceHashesAttestationProofWhenProvided() {
        DeviceService service = new DeviceService(deviceMasterRepository, deviceMasterHistoryRepository, idempotencyRecordRepository, new ObjectMapper());
        DeviceRegistrationRequest request = buildRegistrationRequest();
        request.setAttestationProof("secret");
        when(idempotencyRecordRepository.findByIdempotencyKeyAndRequestHash(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndImeNo(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndDeviceOsId(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndDeviceModelAndDeviceOsVer(any(), any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.save(any(DeviceMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceRegistrationResult result = service.registerDevice(request, " ");

        assertThat(result.isCreated()).isTrue();
        DeviceMaster saved = captureSavedDevice();
        assertThat(saved.getDeviceSecKey()).isEqualTo(DigestUtils.md5DigestAsHex("secret".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void registerDeviceThrowsWhenSerializationFails() throws JsonProcessingException {
        DeviceRegistrationRequest request = buildRegistrationRequest();
        when(objectMapper.writeValueAsString(request)).thenThrow(new JsonProcessingException("error") { });

        assertThatThrownBy(() -> deviceService.registerDevice(request, "key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize request");
    }

    @Test
    void registerDeviceThrowsWhenCachedResponseCannotBeRead() throws Exception {
        DeviceRegistrationRequest request = buildRegistrationRequest();
        String serialized = "payload";
        when(objectMapper.writeValueAsString(request)).thenReturn(serialized);
        String hash = DigestUtils.md5DigestAsHex(serialized.getBytes(StandardCharsets.UTF_8));
        IdempotencyRecord record = new IdempotencyRecord();
        record.setResponsePayload("{}");
        when(idempotencyRecordRepository.findByIdempotencyKeyAndRequestHash("key", hash))
                .thenReturn(Optional.of(record));
        when(objectMapper.readValue(record.getResponsePayload(), DeviceResponse.class))
                .thenThrow(new JsonProcessingException("bad") { });

        assertThatThrownBy(() -> deviceService.registerDevice(request, "key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cached response");
    }

    @Test
    void registerDeviceThrowsWhenIdempotentResponseCannotBePersisted() throws Exception {
        DeviceRegistrationRequest request = buildRegistrationRequest();
        when(objectMapper.writeValueAsString(isA(DeviceRegistrationRequest.class))).thenReturn("payload");
        when(objectMapper.writeValueAsString(isA(DeviceResponse.class))).thenThrow(new JsonProcessingException("bad") { });
        when(idempotencyRecordRepository.findByIdempotencyKeyAndRequestHash(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndImeNo(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndDeviceOsId(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndDeviceModelAndDeviceOsVer(any(), any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.save(any(DeviceMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> deviceService.registerDevice(request, "key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("persist idempotent response");
    }

    @Test
    void enableBiometricsUpdatesDeviceAndHashesSecret() {
        DeviceService service = new DeviceService(deviceMasterRepository, deviceMasterHistoryRepository, idempotencyRecordRepository, new ObjectMapper());
        DeviceMaster device = prepareDeviceEntity("123");
        device.setBiometricType(BiometricType.NONE);
        when(deviceMasterRepository.findById("device"))
                .thenReturn(Optional.of(device));
        when(deviceMasterRepository.save(any(DeviceMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BiometricRegistrationRequest request = new BiometricRegistrationRequest();
        request.setDeviceId("device");
        request.setCid("123");
        request.setBiometricType("FINGERPRINT");
        request.setTouchEnabled(true);
        request.setAttestationProof("proof");

        DeviceResponse response = service.enableBiometrics(request);

        assertThat(response.getBiometricType()).isEqualTo(BiometricType.FINGERPRINT.name());
        assertThat(captureSavedDevice().getDeviceSecKey())
                .isEqualTo(DigestUtils.md5DigestAsHex("proof".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void enableBiometricsThrowsWhenDeviceNotFound() {
        when(deviceMasterRepository.findById("device")).thenReturn(Optional.empty());
        BiometricRegistrationRequest request = new BiometricRegistrationRequest();
        request.setDeviceId("device");
        request.setCid("123");
        request.setBiometricType("FINGERPRINT");

        assertThatThrownBy(() -> deviceService.enableBiometrics(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void enableBiometricsThrowsWhenDifferentBiometricRegistered() {
        DeviceMaster device = prepareDeviceEntity("123");
        device.setBiometricType(BiometricType.FACE);
        when(deviceMasterRepository.findById("device")).thenReturn(Optional.of(device));
        BiometricRegistrationRequest request = new BiometricRegistrationRequest();
        request.setDeviceId("device");
        request.setCid("123");
        request.setBiometricType("FINGERPRINT");

        assertThatThrownBy(() -> deviceService.enableBiometrics(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void detagDevicePerformsSoftDeleteWhenRequested() {
        DeviceService service = new DeviceService(deviceMasterRepository, deviceMasterHistoryRepository, idempotencyRecordRepository, new ObjectMapper());
        DeviceMaster device = prepareDeviceEntity("123");
        when(deviceMasterRepository.findById("device")).thenReturn(Optional.of(device));
        when(deviceMasterHistoryRepository.save(any(DeviceMasterHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceMasterRepository.save(any(DeviceMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DeviceDetagRequest request = new DeviceDetagRequest();
        request.setDeviceId("device");
        request.setCid("123");
        request.setReason("user request");
        request.setHardDelete(false);
        request.setRevokeSessions(true);

        DeviceResponse response = service.detagDevice(request);

        assertThat(response.getStatus()).isEqualTo("I");
        verify(deviceMasterRepository, never()).delete(any(DeviceMaster.class));
        verify(deviceMasterRepository).save(any(DeviceMaster.class));
    }

    @Test
    void detagDevicePerformsHardDeleteWhenRequested() {
        DeviceService service = new DeviceService(deviceMasterRepository, deviceMasterHistoryRepository, idempotencyRecordRepository, new ObjectMapper());
        DeviceMaster device = prepareDeviceEntity("123");
        when(deviceMasterRepository.findById("device")).thenReturn(Optional.of(device));
        when(deviceMasterHistoryRepository.save(any(DeviceMasterHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DeviceDetagRequest request = new DeviceDetagRequest();
        request.setDeviceId("device");
        request.setCid("123");
        request.setReason("fraud");
        request.setHardDelete(true);
        request.setRevokeSessions(false);

        DeviceResponse response = service.detagDevice(request);

        assertThat(response.getStatus()).isEqualTo("I");
        verify(deviceMasterRepository).delete(device);
        verify(deviceMasterRepository, never()).save(any(DeviceMaster.class));
    }

    @Test
    void detagDeviceThrowsWhenNotFound() {
        when(deviceMasterRepository.findById("device")).thenReturn(Optional.empty());
        DeviceDetagRequest request = new DeviceDetagRequest();
        request.setDeviceId("device");
        request.setCid("123");

        assertThatThrownBy(() -> deviceService.detagDevice(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerDeviceSkipsIdempotencyWhenKeyBlank() {
        DeviceService service = new DeviceService(deviceMasterRepository, deviceMasterHistoryRepository, idempotencyRecordRepository, new ObjectMapper());
        DeviceRegistrationRequest request = buildRegistrationRequest();
        when(deviceMasterRepository.findFirstByCidAndImeNo(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndDeviceOsId(any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.findFirstByCidAndDeviceModelAndDeviceOsVer(any(), any(), any())).thenReturn(Optional.empty());
        when(deviceMasterRepository.save(any(DeviceMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerDevice(request, "  ");

        verifyNoInteractions(idempotencyRecordRepository);
    }

    private DeviceRegistrationRequest buildRegistrationRequest() {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setCid("123");
        request.setDeviceOs("ANDROID");
        request.setDeviceOsVer("14");
        request.setModel("Pixel");
        request.setAppVersion("1.0.0");
        request.setLang("en");
        request.setImeNo("IMEI");
        request.setDeviceOsId("OS-ID");
        request.setUserAgent("Agent");
        request.setTouchEnabled(true);
        request.setBiometricType("NONE");
        request.setDeviceNickName("My Phone");
        return request;
    }

    private DeviceMaster prepareDeviceEntity(String cid) {
        DeviceMaster device = DeviceMaster.newDevice(cid);
        device.setDeviceOs("ANDROID");
        device.setDeviceOsVer("14");
        device.setDeviceModel("Pixel");
        device.setAppVersion("1.0.0");
        device.setLang("en");
        device.setDeviceNickName("My Phone");
        device.setImeNo("IMEI");
        device.setDeviceOsId("OS-ID");
        device.setStatus("A");
        device.setTouchEnabled(true);
        device.setBiometricType(BiometricType.NONE);
        device.setLastUpdateDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        return device;
    }

    private DeviceMaster captureSavedDevice() {
        ArgumentCaptor<DeviceMaster> captor = ArgumentCaptor.forClass(DeviceMaster.class);
        verify(deviceMasterRepository).save(captor.capture());
        return captor.getValue();
    }
}
