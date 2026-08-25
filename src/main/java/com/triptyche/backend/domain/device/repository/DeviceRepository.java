package com.triptyche.backend.domain.device.repository;

import com.triptyche.backend.domain.device.model.Device;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

  Optional<Device> findByToken(String token);

  List<Device> findAllByUserId(Long userId);

  void deleteByToken(String token);

  void deleteByTokenIn(List<String> tokens);
}
