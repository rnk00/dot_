package com.dot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 유저×기능×날짜(KST) 단위 AI 사용 횟수 카운터
@Entity
@Table(name = "ai_usage_counters",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "feature", "usage_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageCounter {

    public enum Feature { TRY_RECOMMENDATION, INSIGHT, REFINE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Feature feature;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer count = 0;
}
