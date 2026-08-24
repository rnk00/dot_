package com.dot.controller;

import com.dot.dto.RetrospectDto;
import com.dot.service.RetrospectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/retrospects")
@RequiredArgsConstructor
public class RetrospectController {

    private final RetrospectService retrospectService;

    // 월별 캘린더 데이터
    @GetMapping("/calendar")
    public ResponseEntity<List<RetrospectDto.CalendarItem>> getCalendar(
            @AuthenticationPrincipal Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(retrospectService.getCalendarData(userId, year, month));
    }

    // 특정 날짜 조회
    @GetMapping("/date/{date}")
    public ResponseEntity<RetrospectDto.Response> getByDate(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        RetrospectDto.Response response = retrospectService.getByDate(userId, date);
        if (response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }

    // 점수 생성/수정 (회고가 없으면 이때 생성됨)
    @PutMapping("/date/{date}/score")
    public ResponseEntity<RetrospectDto.Response> upsertScore(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody RetrospectDto.ScoreRequest request) {
        return ResponseEntity.ok(retrospectService.upsertScore(userId, date, request));
    }

    // 삭제 (기간 제한 없음)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        retrospectService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    // KPT 항목 추가
    @PostMapping("/date/{date}/kpt-items")
    public ResponseEntity<RetrospectDto.Response> addItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody RetrospectDto.ItemCreateRequest request) {
        return ResponseEntity.ok(retrospectService.addItem(userId, date, request));
    }

    // KPT 항목 수정
    @PutMapping("/date/{date}/kpt-items/{itemId}")
    public ResponseEntity<RetrospectDto.Response> updateItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long itemId,
            @Valid @RequestBody RetrospectDto.ItemUpdateRequest request) {
        return ResponseEntity.ok(retrospectService.updateItem(userId, date, itemId, request));
    }

    // KPT 항목 삭제
    @DeleteMapping("/date/{date}/kpt-items/{itemId}")
    public ResponseEntity<RetrospectDto.Response> deleteItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(retrospectService.deleteItem(userId, date, itemId));
    }

    // KPT 항목 순서 변경
    @PutMapping("/date/{date}/kpt-items/order")
    public ResponseEntity<RetrospectDto.Response> reorderItems(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody RetrospectDto.ItemOrderRequest request) {
        return ResponseEntity.ok(retrospectService.reorderItems(userId, date, request));
    }
}
