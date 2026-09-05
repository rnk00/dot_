package com.dot.service;

import com.dot.dto.KptMemoDto;
import com.dot.entity.KptMemo;
import com.dot.entity.KptType;
import com.dot.entity.User;
import com.dot.exception.ApiException;
import com.dot.repository.KptMemoRepository;
import com.dot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 날짜/회고와 무관한 독립적인 K/P/T 메모 (KPT 노트)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KptMemoService {

    private static final int MAX_MEMOS_PER_TYPE = 20;

    private final KptMemoRepository kptMemoRepository;
    private final UserRepository userRepository;

    public KptMemoDto.ListResponse listAll(Long userId) {
        return KptMemoDto.ListResponse.builder()
                .keep(list(userId, KptType.KEEP))
                .problem(list(userId, KptType.PROBLEM))
                .tryItems(list(userId, KptType.TRY))
                .build();
    }

    private List<KptMemoDto.Item> list(Long userId, KptType type) {
        return kptMemoRepository.findByUserIdAndTypeOrderByOrderIndexAsc(userId, type)
                .stream()
                .map(KptMemoDto.Item::from)
                .toList();
    }

    @Transactional
    public void create(Long userId, KptType type, String content) {
        if (content == null || content.isBlank()) {
            throw ApiException.badRequest("내용을 입력해주세요.");
        }
        int count = kptMemoRepository.countByUserIdAndType(userId, type);
        if (count >= MAX_MEMOS_PER_TYPE) {
            throw ApiException.badRequest("최대 20개까지 작성 가능합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없습니다."));

        KptMemo memo = KptMemo.builder()
                .user(user)
                .type(type)
                .content(content)
                .orderIndex(count)
                .build();
        kptMemoRepository.save(memo);
    }

    @Transactional
    public void update(Long userId, Long id, String content) {
        KptMemo memo = getOwned(userId, id);
        if (content == null || content.isBlank()) {
            KptType type = memo.getType();
            kptMemoRepository.delete(memo);
            reindex(userId, type);
        } else {
            memo.setContent(content);
        }
    }

    @Transactional
    public void delete(Long userId, Long id) {
        KptMemo memo = getOwned(userId, id);
        KptType type = memo.getType();
        kptMemoRepository.delete(memo);
        reindex(userId, type);
    }

    @Transactional
    public void reorder(Long userId, KptType type, List<Long> orderedIds) {
        List<KptMemo> memos = kptMemoRepository.findByUserIdAndTypeOrderByOrderIndexAsc(userId, type);
        if (memos.size() != orderedIds.size()) {
            throw ApiException.badRequest("항목 목록이 일치하지 않습니다.");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            KptMemo memo = memos.stream().filter(m -> m.getId().equals(id)).findFirst()
                    .orElseThrow(() -> ApiException.badRequest("항목 목록이 일치하지 않습니다."));
            memo.setOrderIndex(i);
        }
    }

    private KptMemo getOwned(Long userId, Long id) {
        KptMemo memo = kptMemoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("메모를 찾을 수 없습니다."));
        if (!memo.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("권한이 없습니다.");
        }
        return memo;
    }

    private void reindex(Long userId, KptType type) {
        List<KptMemo> memos = kptMemoRepository.findByUserIdAndTypeOrderByOrderIndexAsc(userId, type);
        for (int i = 0; i < memos.size(); i++) {
            memos.get(i).setOrderIndex(i);
        }
    }
}
