package com.buffturf.buffturf_backend.repository;

import com.buffturf.buffturf_backend.model.QrPass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QrPassRepository extends JpaRepository<QrPass, Long> {
    Optional<QrPass> findByQrToken(String qrToken);
}
