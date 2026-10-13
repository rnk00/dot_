package com.dot.repository;

import com.dot.entity.AiUsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface AiUsageCounterRepository extends JpaRepository<AiUsageCounter, Long> {

    Optional<AiUsageCounter> findByUserIdAndFeatureAndUsageDate(Long userId, AiUsageCounter.Feature feature, LocalDate usageDate);

    @Modifying
    @Query("delete from AiUsageCounter c where c.userId = :userId")
    void deleteByUserId(Long userId);
}
