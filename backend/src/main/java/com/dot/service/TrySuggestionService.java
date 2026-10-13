package com.dot.service;

import com.dot.dto.AIDto;
import com.dot.entity.TrySuggestion;
import com.dot.repository.TrySuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 회고 날짜별 AI Try 추천 히스토리.
 * 회고는 14일이 지나면 읽기 전용이 돼서 추천을 더 이상 추가할 수 없으므로, 그때 함께 지운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrySuggestionService {

    // RetrospectService의 작성 가능 기간과 같아야 함
    private static final int WRITABLE_DAYS = 14;

    private final TrySuggestionRepository repository;

    public static boolean isWritable(LocalDate date) {
        return !date.isBefore(LocalDate.now().minusDays(WRITABLE_DAYS));
    }

    @Transactional
    public AIDto.SuggestionBatch save(Long userId, LocalDate date, List<String> suggestions) {
        String batchId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        repository.saveAll(suggestions.stream()
                .map(content -> TrySuggestion.builder()
                        .userId(userId).date(date).batchId(batchId).content(content).createdAt(now).build())
                .toList());
        return AIDto.SuggestionBatch.builder().batchId(batchId).createdAt(now).suggestions(suggestions).build();
    }

    /** 최신 추천 묶음부터 */
    public List<AIDto.SuggestionBatch> history(Long userId, LocalDate date) {
        if (!isWritable(date)) return List.of();
        Map<String, AIDto.SuggestionBatch> batches = new LinkedHashMap<>();
        for (TrySuggestion s : repository.findByUserIdAndDateOrderByCreatedAtDescIdAsc(userId, date)) {
            batches.computeIfAbsent(s.getBatchId(), id -> AIDto.SuggestionBatch.builder()
                    .batchId(id).createdAt(s.getCreatedAt()).suggestions(new ArrayList<>()).build())
                    .getSuggestions().add(s.getContent());
        }
        return new ArrayList<>(batches.values());
    }

    /** 매일 0시 10분(KST), 작성 기간이 지난 날짜의 추천을 삭제 */
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpired() {
        int deleted = repository.deleteByDateBefore(LocalDate.now().minusDays(WRITABLE_DAYS));
        if (deleted > 0) log.info("작성 기간이 지난 AI Try 추천 {}건 삭제", deleted);
    }
}
