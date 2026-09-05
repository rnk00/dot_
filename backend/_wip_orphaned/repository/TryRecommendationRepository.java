package com.dot.repository;

import com.dot.entity.TryRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TryRecommendationRepository extends JpaRepository<TryRecommendation, Long> {
    List<TryRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
