package com.dot.util;

import java.time.LocalDate;

// 회고/KPT 항목의 작성·수정 가능 기간 규칙 (삭제에는 적용하지 않음)
public final class RetrospectDatePolicy {

    public static final int EDIT_WINDOW_DAYS = 14;

    private RetrospectDatePolicy() {}

    public static boolean isFuture(LocalDate date) {
        return date.isAfter(LocalDate.now());
    }

    public static boolean isEditable(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate earliest = today.minusDays(EDIT_WINDOW_DAYS);
        return !date.isBefore(earliest) && !date.isAfter(today);
    }
}
