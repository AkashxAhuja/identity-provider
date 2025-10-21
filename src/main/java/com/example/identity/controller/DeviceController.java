package com.example.identity.controller;

import com.example.identity.dto.BiometricRegistrationRequest;
import com.example.identity.dto.DeviceDetagRequest;
import com.example.identity.dto.DeviceRegistrationRequest;
import com.example.identity.dto.DeviceResponse;
import com.example.identity.service.DeviceRegistrationResult;
import com.example.identity.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request,
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
        DeviceRegistrationResult result = deviceService.registerDevice(request, idempotencyKey);
        HttpStatus status = result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.getDeviceResponse());
    }

    @PostMapping("/biometrics")
    public ResponseEntity<DeviceResponse> registerBiometrics(@Valid @RequestBody BiometricRegistrationRequest request) {
        DeviceResponse response = deviceService.enableBiometrics(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/detag")
    public ResponseEntity<DeviceResponse> detagDevice(@Valid @RequestBody DeviceDetagRequest request) {
        DeviceResponse response = deviceService.detagDevice(request);
        return ResponseEntity.ok(response);
    }
}
