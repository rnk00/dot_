package com.dot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// AI가 추천한 Try 한 줄. 같은 요청에서 나온 추천들은 batchId가 같음.
// 회고 날짜가 작성 가능 기간(14일)을 지나면 더 이상 추가할 수 없으므로 자동 삭제됨
@Entity
@Table(name = "try_suggestions", indexes = @Index(columnList = "user_id, date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrySuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
