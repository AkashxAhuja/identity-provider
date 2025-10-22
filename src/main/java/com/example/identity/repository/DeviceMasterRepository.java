package com.example.identity.repository;

import com.example.identity.model.DeviceMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceMasterRepository extends JpaRepository<DeviceMaster, String> {

    Optional<DeviceMaster> findFirstByCidAndImeNo(String cid, String imeNo);

    Optional<DeviceMaster> findFirstByCidAndDeviceOsId(String cid, String deviceOsId);

    Optional<DeviceMaster> findFirstByCidAndDeviceModelAndDeviceOsVer(String cid, String deviceModel, String deviceOsVer);
}
