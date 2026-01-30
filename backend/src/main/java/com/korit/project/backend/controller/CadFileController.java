package com.korit.project.backend.controller;

import com.korit.project.backend.dto.CadFileResponse;
import com.korit.project.backend.dto.PartResponse;
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
import java.util.List;

@RestController
@RequestMapping("/api/cad")
@RequiredArgsConstructor
public class CadFileController {

    private final CadFileService cadFileService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("파일이 비어있습니다.");
            }
            
            CadFileResponse response = cadFileService.uploadCadFile(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<CadFileResponse>> getAllCadFiles() {
        return ResponseEntity.ok(cadFileService.getAllCadFiles());
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<CadFileResponse> getCadFileById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(cadFileService.getCadFileById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/files/{id}/glb")
    public ResponseEntity<Resource> downloadGlbFile(@PathVariable Long id) {
        try {
            File glbFile = cadFileService.getGlbFile(id);
            
            // 파일이 실제로 존재하는지 확인
            if (!glbFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(glbFile);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("model/gltf-binary"))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/files/{id}/parts")
    public ResponseEntity<List<PartResponse>> getPartsByCadFileId(@PathVariable Long id) {
        return ResponseEntity.ok(cadFileService.getPartsByCadFileId(id));
    }
}
