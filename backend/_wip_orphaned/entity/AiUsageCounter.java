package com.dot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 유저×기능×날짜(KST) 단위 AI 생성 횟수 카운터
@Entity
@Table(name = "ai_usage_counters", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "feature", "usage_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiFeature feature;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer count = 0;
}
