package com.dot.service;

import com.dot.entity.AiFeature;
import com.dot.entity.AiUsageCounter;
import com.dot.exception.ApiException;
import com.dot.repository.AiUsageCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

// KST 자정 기준으로 초기화되는 유저별 AI 기능 하루 사용 횟수 관리
@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AiUsageCounterRepository repository;

    @Transactional
    public int consume(Long userId, AiFeature feature, int dailyLimit) {
        LocalDate today = LocalDate.now(KST);
        AiUsageCounter counter = repository.findByUserIdAndFeatureAndUsageDate(userId, feature, today)
                .orElseGet(() -> repository.save(
                        AiUsageCounter.builder().userId(userId).feature(feature).usageDate(today).count(0).build()));

        if (counter.getCount() >= dailyLimit) {
            throw ApiException.badRequest("오늘 생성 가능 횟수를 모두 사용했습니다.");
        }
        counter.setCount(counter.getCount() + 1);
        return dailyLimit - counter.getCount();
    }

    public int remaining(Long userId, AiFeature feature, int dailyLimit) {
        LocalDate today = LocalDate.now(KST);
        int used = repository.findByUserIdAndFeatureAndUsageDate(userId, feature, today)
                .map(AiUsageCounter::getCount)
                .orElse(0);
        return Math.max(0, dailyLimit - used);
    }
}
