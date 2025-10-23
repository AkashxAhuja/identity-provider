package com.example.identity.controller;

import com.example.identity.dto.BiometricRegistrationRequest;
import com.example.identity.dto.DeviceDetagRequest;
import com.example.identity.dto.DeviceRegistrationRequest;
import com.example.identity.dto.DeviceResponse;
import com.example.identity.service.DeviceRegistrationResult;
import com.example.identity.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private DeviceController controller;

    @Test
    void registerDeviceReturnsCreatedWhenNewDevice() {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        DeviceResponse response = buildResponse();
        when(deviceService.registerDevice(request, "key"))
                .thenReturn(new DeviceRegistrationResult(response, true));

        var entity = controller.registerDevice(request, "key");

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getBody()).isEqualTo(response);
        verify(deviceService).registerDevice(request, "key");
    }

    @Test
    void registerDeviceReturnsOkWhenExisting() {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        DeviceResponse response = buildResponse();
        when(deviceService.registerDevice(request, null))
                .thenReturn(new DeviceRegistrationResult(response, false));

        var entity = controller.registerDevice(request, null);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deviceService).registerDevice(request, null);
    }

    @Test
    void registerBiometricsDelegatesToService() {
        BiometricRegistrationRequest request = new BiometricRegistrationRequest();
        DeviceResponse response = buildResponse();
        when(deviceService.enableBiometrics(request)).thenReturn(response);

        var entity = controller.registerBiometrics(request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(response);
        verify(deviceService).enableBiometrics(request);
    }

    @Test
    void detagDeviceDelegatesToService() {
        DeviceDetagRequest request = new DeviceDetagRequest();
        DeviceResponse response = buildResponse();
        when(deviceService.detagDevice(request)).thenReturn(response);

        var entity = controller.detagDevice(request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(response);
        verify(deviceService).detagDevice(request);
    }

    private DeviceResponse buildResponse() {
        DeviceResponse response = new DeviceResponse();
        response.setDeviceId("device");
        response.setCid("123");
        response.setStatus("A");
        response.setLastUpdateDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        return response;
    }
}
