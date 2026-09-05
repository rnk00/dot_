package com.dot.repository;

import com.dot.entity.KptMemo;
import com.dot.entity.KptType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KptMemoRepository extends JpaRepository<KptMemo, Long> {
    List<KptMemo> findByUserIdAndTypeOrderByOrderIndexAsc(Long userId, KptType type);
    int countByUserIdAndType(Long userId, KptType type);
}
