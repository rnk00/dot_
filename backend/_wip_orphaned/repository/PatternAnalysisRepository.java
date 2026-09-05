package com.dot.repository;

import com.dot.entity.PatternAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatternAnalysisRepository extends JpaRepository<PatternAnalysis, Long> {
    List<PatternAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PatternAnalysis> findFirstByUserIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
            Long userId, LocalDate periodStart, LocalDate periodEnd);
}
