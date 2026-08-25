package com.dot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "retrospects", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Retrospect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @OneToMany(mappedBy = "retrospect", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<KptItem> items = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer score = 3;  // 1~5

    @Column(name = "color_theme")
    @Builder.Default
    private String colorTheme = "default";

    @Column(name = "is_github_synced", nullable = false)
    @Builder.Default
    private Boolean isGithubSynced = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
