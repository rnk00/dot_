package com.dot.controller;

import com.dot.dto.KptMemoDto;
import com.dot.service.KptMemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kpt-notes")
@RequiredArgsConstructor
public class KptMemoController {

    private final KptMemoService kptMemoService;

    @GetMapping
    public ResponseEntity<KptMemoDto.ListResponse> list(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(kptMemoService.listAll(userId));
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody KptMemoDto.CreateRequest request) {
        kptMemoService.create(userId, request.getType(), request.getContent());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody KptMemoDto.UpdateRequest request) {
        kptMemoService.update(userId, id, request.getContent());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        kptMemoService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/order")
    public ResponseEntity<Void> reorder(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody KptMemoDto.OrderRequest request) {
        kptMemoService.reorder(userId, request.getType(), request.getOrderedIds());
        return ResponseEntity.ok().build();
    }
}
