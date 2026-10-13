package com.dot.repository;

import com.dot.entity.KptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KptItemRepository extends JpaRepository<KptItem, Long> {

    List<KptItem> findByRetrospectIdAndTypeOrderByOrderIndexAsc(Long retrospectId, KptItem.Type type);

    List<KptItem> findByRetrospectIdOrderByOrderIndexAsc(Long retrospectId);

    long countByRetrospectIdAndType(Long retrospectId, KptItem.Type type);

    // 캘린더(한 달치) 조회용 — 회고마다 따로 조회하지 않고 한 번에 가져옴
    List<KptItem> findByRetrospectIdInOrderByOrderIndexAsc(List<Long> retrospectIds);
}
