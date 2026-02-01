package com.korit.project.backend.controller;

import com.korit.project.backend.dto.resp.ApiRespDto;
import com.korit.project.backend.service.CadFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * CAD 파일 관리 Controller
 */
@RestController
@RequestMapping("/api/cad")
@RequiredArgsConstructor
public class CadFileController {

    private final CadFileService cadFileService;

    /**
     * CAD 파일 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiRespDto<?>> uploadCadFile(@RequestParam("file") MultipartFile file) {
        ApiRespDto<?> response = cadFileService.uploadCadFile(file);
        if ("failed".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 업로드된 파일 목록 조회
     */
    @GetMapping("/files")
    public ResponseEntity<ApiRespDto<?>> getAllCadFiles() {
        return ResponseEntity.ok(cadFileService.getAllCadFiles());
    }

    /**
     * 파일 상세 조회 (부품 정보 포함)
     */
    @GetMapping("/files/{id}")
    public ResponseEntity<ApiRespDto<?>> getCadFileById(@PathVariable Long id) {
        ApiRespDto<?> response = cadFileService.getCadFileById(id);
        if ("failed".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * GLB 파일 다운로드
     */
    @GetMapping("/files/{id}/glb")
    public ResponseEntity<Resource> downloadGlbFile(@PathVariable Long id) {
        File glbFile = cadFileService.getGlbFile(id);
        if (!glbFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(glbFile);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("model/gltf-binary"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }
}
