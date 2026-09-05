package com.dot.repository;

import com.dot.entity.AiFeature;
import com.dot.entity.AiUsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiUsageCounterRepository extends JpaRepository<AiUsageCounter, Long> {
    Optional<AiUsageCounter> findByUserIdAndFeatureAndUsageDate(Long userId, AiFeature feature, LocalDate usageDate);
}
