package com.dot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 기간별 반복 키워드 분석 결과 (히스토리로 계속 쌓임, 덮어쓰지 않음)
@Entity
@Table(name = "pattern_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatternAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    // KeywordItem 목록을 JSON 문자열로 저장 (AI의 비정형 산출물이라 별도 테이블 없이 통으로 저장/조회)
    @Column(name = "keywords_json", columnDefinition = "TEXT", nullable = false)
    private String keywordsJson;

    @Column(name = "warning_pattern", columnDefinition = "TEXT")
    private String warningPattern;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
