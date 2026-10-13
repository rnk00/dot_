package com.dot.service;

import com.dot.entity.AiUsageCounter;
import com.dot.entity.AiUsageCounter.Feature;
import com.dot.repository.AiUsageCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 유저별 AI 기능 하루 사용 횟수 관리 (한국 시간 자정에 초기화)
 */
@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AiUsageCounterRepository repository;

    public int remaining(Long userId, Feature feature, int dailyLimit) {
        return Math.max(0, dailyLimit - used(userId, feature));
    }

    /** 남은 횟수가 없으면 429로 막음 (AI 호출 전에 확인) */
    public void checkAvailable(Long userId, Feature feature, int dailyLimit) {
        if (used(userId, feature) >= dailyLimit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "오늘 사용할 수 있는 횟수를 모두 썼어요. 내일 자정에 다시 채워져요.");
        }
    }

    /** AI 호출이 성공했을 때만 1회 차감하고, 남은 횟수를 돌려줌 (실패한 요청은 횟수에서 빼지 않음) */
    @Transactional
    public int consume(Long userId, Feature feature, int dailyLimit) {
        LocalDate today = LocalDate.now(KST);
        AiUsageCounter counter = repository.findByUserIdAndFeatureAndUsageDate(userId, feature, today)
                .orElseGet(() -> AiUsageCounter.builder()
                        .userId(userId).feature(feature).usageDate(today).count(0).build());
        counter.setCount(counter.getCount() + 1);
        repository.save(counter);
        return Math.max(0, dailyLimit - counter.getCount());
    }

    private int used(Long userId, Feature feature) {
        return repository.findByUserIdAndFeatureAndUsageDate(userId, feature, LocalDate.now(KST))
                .map(AiUsageCounter::getCount)
                .orElse(0);
    }
}
