package com.dot.service;

import com.dot.dto.RetrospectDto;
import com.dot.entity.KptItem;
import com.dot.entity.Retrospect;
import com.dot.entity.User;
import com.dot.repository.KptItemRepository;
import com.dot.repository.RetrospectRepository;
import com.dot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetrospectService {

    private static final int READONLY_DAYS = 14;
    private static final int MAX_ITEMS_PER_TYPE = 20;

    private final RetrospectRepository retrospectRepository;
    private final UserRepository userRepository;
    private final KptItemRepository kptItemRepository;

    // 월별 캘린더 데이터 (날짜 + 점수만)
    public List<RetrospectDto.CalendarItem> getCalendarData(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return retrospectRepository.findByUserIdAndDateBetween(userId, startDate, endDate)
                .stream()
                .map(r -> RetrospectDto.CalendarItem.builder()
                        .id(r.getId())
                        .date(r.getDate())
                        .score(r.getScore())
                        .colorTheme(r.getColorTheme())
                        .build())
                .toList();
    }

    // 특정 날짜 회고 조회 (없으면 null → 컨트롤러가 404)
    public RetrospectDto.Response getByDate(Long userId, LocalDate date) {
        validateNotFuture(date);
        return retrospectRepository.findByUserIdAndDate(userId, date)
                .map(RetrospectDto.Response::from)
                .orElse(null);
    }

    // 점수 생성/수정 (회고가 없으면 이 시점에 생성)
    @Transactional
    public RetrospectDto.Response upsertScore(Long userId, LocalDate date, RetrospectDto.ScoreRequest request) {
        validateNotFuture(date);
        validateWritable(date);

        Retrospect retrospect = retrospectRepository.findByUserIdAndDate(userId, date)
                .orElseGet(() -> createBlank(userId, date));

        retrospect.setScore(request.getScore());
        retrospect.setIsGithubSynced(false);
        return RetrospectDto.Response.from(retrospectRepository.save(retrospect));
    }

    // 삭제 — 기간 제한 없음
    @Transactional
    public void delete(Long userId, Long id) {
        Retrospect retrospect = getOwned(id, userId);
        retrospectRepository.delete(retrospect);
    }

    // KPT 항목 추가
    @Transactional
    public RetrospectDto.Response addItem(Long userId, LocalDate date, RetrospectDto.ItemCreateRequest request) {
        validateNotFuture(date);
        validateWritable(date);
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용을 입력해주세요.");
        }

        Retrospect retrospect = retrospectRepository.findByUserIdAndDate(userId, date)
                .orElseGet(() -> createBlank(userId, date));

        long count = kptItemRepository.countByRetrospectIdAndType(retrospect.getId(), request.getType());
        if (count >= MAX_ITEMS_PER_TYPE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최대 20개까지 작성 가능합니다.");
        }

        KptItem item = KptItem.builder()
                .retrospect(retrospect)
                .type(request.getType())
                .content(request.getContent().trim())
                .orderIndex((int) count)
                .build();
        kptItemRepository.save(item);

        retrospect.setIsGithubSynced(false);
        retrospectRepository.save(retrospect);

        return RetrospectDto.Response.from(reload(retrospect.getId()));
    }

    // KPT 항목 수정
    @Transactional
    public RetrospectDto.Response updateItem(Long userId, LocalDate date, Long itemId, RetrospectDto.ItemUpdateRequest request) {
        validateNotFuture(date);
        validateWritable(date);
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용을 입력해주세요.");
        }

        KptItem item = getOwnedItem(userId, date, itemId);
        item.setContent(request.getContent().trim());
        kptItemRepository.save(item);

        item.getRetrospect().setIsGithubSynced(false);
        retrospectRepository.save(item.getRetrospect());

        return RetrospectDto.Response.from(reload(item.getRetrospect().getId()));
    }

    // KPT 항목 삭제
    @Transactional
    public RetrospectDto.Response deleteItem(Long userId, LocalDate date, Long itemId) {
        validateNotFuture(date);
        validateWritable(date);

        KptItem item = getOwnedItem(userId, date, itemId);
        Long retrospectId = item.getRetrospect().getId();
        kptItemRepository.delete(item);

        Retrospect retrospect = reload(retrospectId);
        retrospect.setIsGithubSynced(false);
        retrospectRepository.save(retrospect);

        return RetrospectDto.Response.from(reload(retrospectId));
    }

    // KPT 항목 순서 변경
    @Transactional
    public RetrospectDto.Response reorderItems(Long userId, LocalDate date, RetrospectDto.ItemOrderRequest request) {
        validateNotFuture(date);
        validateWritable(date);

        Retrospect retrospect = retrospectRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다."));

        List<KptItem> items = kptItemRepository.findByRetrospectIdAndTypeOrderByOrderIndexAsc(retrospect.getId(), request.getType());
        List<Long> orderedIds = request.getOrderedIds();

        for (KptItem item : items) {
            int newIndex = orderedIds.indexOf(item.getId());
            if (newIndex < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "순서 목록이 유효하지 않습니다.");
            }
            item.setOrderIndex(newIndex);
        }
        kptItemRepository.saveAll(items);

        return RetrospectDto.Response.from(reload(retrospect.getId()));
    }

    // 동시 요청이 겹쳐서 같은 유저+날짜로 동시에 생성을 시도할 수 있음 —
    // DB 유니크 제약(user_id, date)에 걸리면 그새 다른 요청이 만든 걸로 다시 조회해서 반환
    private Retrospect createBlank(Long userId, LocalDate date) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
            Retrospect retrospect = Retrospect.builder()
                    .user(user)
                    .date(date)
                    .score(3)
                    .build();
            return retrospectRepository.saveAndFlush(retrospect);
        } catch (DataIntegrityViolationException e) {
            return retrospectRepository.findByUserIdAndDate(userId, date)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "회고 생성 중 오류가 발생했습니다."));
        }
    }

    private Retrospect reload(Long id) {
        return retrospectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다."));
    }

    private Retrospect getOwned(Long id, Long userId) {
        Retrospect retrospect = retrospectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다."));
        if (!retrospect.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
        return retrospect;
    }

    private KptItem getOwnedItem(Long userId, LocalDate date, Long itemId) {
        Retrospect retrospect = retrospectRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다."));
        KptItem item = kptItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "항목을 찾을 수 없습니다."));
        if (!item.getRetrospect().getId().equals(retrospect.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
        return item;
    }

    private void validateNotFuture(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "미래 날짜에는 접근할 수 없습니다.");
        }
    }

    private void validateWritable(LocalDate date) {
        if (date.isBefore(LocalDate.now().minusDays(READONLY_DAYS))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성 후 14일이 지나 읽기 전용입니다.");
        }
    }
}
