package com.korit.project.backend.controller;

import com.korit.project.backend.dto.NoteRequest;
import com.korit.project.backend.dto.PartRenameRequest;
import com.korit.project.backend.dto.resp.ApiRespDto;
import com.korit.project.backend.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 부품 관리 Controller
 */
@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    /**
     * 부품 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiRespDto<?>> getPartById(@PathVariable Long id) {
        ApiRespDto<?> response = partService.getPartById(id);
        if ("failed".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 부품 메모 저장/수정
     */
    @PostMapping("/{id}/note")
    public ResponseEntity<ApiRespDto<?>> saveOrUpdateNote(@PathVariable Long id, @RequestBody NoteRequest request) {
        ApiRespDto<?> response = partService.saveOrUpdateNote(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 부품 메모 삭제
     */
    @DeleteMapping("/{id}/note")
    public ResponseEntity<ApiRespDto<?>> deleteNote(@PathVariable Long id) {
        ApiRespDto<?> response = partService.deleteNote(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 부품 이름 변경
     */
    @PatchMapping("/{id}/display-name")
    public ResponseEntity<ApiRespDto<?>> renamePart(@PathVariable Long id, @RequestBody PartRenameRequest request) {
        if (request == null || request.getDisplayName() == null) {
            throw new RuntimeException("부품 이름을 입력해주세요.");
        }
        ApiRespDto<?> response = partService.renamePart(id, request.getDisplayName());
        return ResponseEntity.ok(response);
    }
}
