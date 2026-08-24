package com.dot.repository;

import com.dot.entity.KptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KptItemRepository extends JpaRepository<KptItem, Long> {

    List<KptItem> findByRetrospectIdAndTypeOrderByOrderIndexAsc(Long retrospectId, KptItem.Type type);

    List<KptItem> findByRetrospectIdOrderByOrderIndexAsc(Long retrospectId);

    long countByRetrospectIdAndType(Long retrospectId, KptItem.Type type);
}
