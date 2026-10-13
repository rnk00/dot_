package com.dot.repository;

import com.dot.entity.TrySuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TrySuggestionRepository extends JpaRepository<TrySuggestion, Long> {

    List<TrySuggestion> findByUserIdAndDateOrderByCreatedAtDescIdAsc(Long userId, LocalDate date);

    @Modifying
    @Query("delete from TrySuggestion s where s.date < :date")
    int deleteByDateBefore(LocalDate date);

    @Modifying
    @Query("delete from TrySuggestion s where s.userId = :userId")
    void deleteByUserId(Long userId);
}
