package com.korit.project.backend.service;

import com.korit.project.backend.dto.CadFileResponse;
import com.korit.project.backend.dto.NoteRequest;
import com.korit.project.backend.dto.PartResponse;
import com.korit.project.backend.entity.CadFile;
import com.korit.project.backend.entity.Part;
import com.korit.project.backend.entity.PartNote;
import com.korit.project.backend.mapper.CadFileMapper;
import com.korit.project.backend.mapper.PartMapper;
import com.korit.project.backend.mapper.PartNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CadFileService {

    private final CadFileMapper cadFileMapper;
    private final PartMapper partMapper;
    private final PartNoteMapper partNoteMapper;
    private final PythonWorkerService pythonWorkerService;

    @Value("${file.upload.temp-dir}")
    private String tempDir;

    @Value("${file.upload.converted-dir}")
    private String convertedDir;

    @Transactional
    public CadFileResponse uploadCadFile(MultipartFile file) throws IOException {
        // 파일 확장자 검증 (STL, OBJ, PLY, STEP, STP, IGES 허용)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        boolean allowed =
                extension.equals("stl") ||
                        extension.equals("obj") ||
                        extension.equals("ply") ||
                        extension.equals("step") ||
                        extension.equals("stp") ||
                        extension.equals("igs") ||
                        extension.equals("iges");

        if (!allowed) {
            throw new IllegalArgumentException("STL, OBJ, PLY, STEP, STP, IGES 파일만 업로드 가능합니다.");
        }

        // 임시 디렉토리 생성 (절대 경로로 변환)
        Path tempPath;
        if (Paths.get(tempDir).isAbsolute()) {
            tempPath = Paths.get(tempDir);
        } else {
            // 상대 경로인 경우 프로젝트 루트 기준으로 변환
            String userDir = System.getProperty("user.dir");
            tempPath = Paths.get(userDir, tempDir);
        }
        Files.createDirectories(tempPath);
        log.info("임시 디렉토리: {}", tempPath.toAbsolutePath());

        // 임시 파일 저장
        String tempFileName = System.currentTimeMillis() + "_" + originalFilename;
        Path tempFilePath = tempPath.resolve(tempFileName);
        file.transferTo(tempFilePath.toFile());

        // 파일이 실제로 저장되었는지 확인
        if (!Files.exists(tempFilePath)) {
            throw new IOException("파일 저장에 실패했습니다: " + tempFilePath);
        }
        log.info("파일 저장 완료: {}", tempFilePath.toAbsolutePath());

        // DB에 파일 정보 저장
        CadFile cadFile = new CadFile();
        cadFile.setOriginalFilename(originalFilename);
        cadFile.setFileSize(file.getSize());
        cadFile.setStatus("UPLOADING");
        cadFileMapper.insertCadFile(cadFile);

        Long cadFileId = cadFile.getId();
        log.info("CAD 파일 업로드 완료: ID={}, 파일명={}, 경로={}", cadFileId, originalFilename, tempFilePath.toAbsolutePath());

        // Python Worker 호출 (비동기) - 절대 경로 전달
        try {
            pythonWorkerService.processCadFile(cadFileId, tempFilePath.toAbsolutePath().toString(), originalFilename);
        } catch (Exception e) {
            log.error("Python Worker 호출 실패", e);
            cadFileMapper.updateStatus(cadFileId, "FAILED");
            throw new RuntimeException("파일 처리 중 오류가 발생했습니다.", e);
        }

        return convertToResponse(cadFile);
    }

    public List<CadFileResponse> getAllCadFiles() {
        return cadFileMapper.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public CadFileResponse getCadFileById(Long id) {
        CadFile cadFile = cadFileMapper.findById(id);
        if (cadFile == null) {
            throw new IllegalArgumentException("파일을 찾을 수 없습니다: ID=" + id);
        }
        log.info("파일 조회: ID={}, glbFilePath={}", id, cadFile.getGlbFilePath());
        return convertToResponse(cadFile);
    }

    public File getGlbFile(Long id) {
        CadFile cadFile = cadFileMapper.findById(id);
        if (cadFile == null || cadFile.getGlbFilePath() == null) {
            throw new IllegalArgumentException("GLB 파일을 찾을 수 없습니다: ID=" + id);
        }
        return new File(cadFile.getGlbFilePath());
    }

    public List<PartResponse> getPartsByCadFileId(Long cadFileId) {
        List<Part> parts = partMapper.findByCadFileId(cadFileId);
        return parts.stream()
                .map(part -> {
                    PartResponse response = new PartResponse();
                    response.setId(part.getId());
                    response.setName(part.getName());

                    // ✅ displayName 정책:
                    // - DB display_name이 있으면 그걸 우선 사용
                    // - 없으면 최초 표시는 name으로
                    String resolvedDisplayName =
                            (part.getDisplayName() != null && !part.getDisplayName().isBlank())
                                    ? part.getDisplayName()
                                    : part.getName();
                    response.setDisplayName(resolvedDisplayName);

                    // ✅ 추가: node 매핑 필드 내려주기
                    response.setPartKey(part.getPartKey());
                    response.setNodeIndex(part.getNodeIndex());

                    response.setPositionX(part.getPositionX());
                    response.setPositionY(part.getPositionY());
                    response.setPositionZ(part.getPositionZ());
                    response.setSizeX(part.getSizeX());
                    response.setSizeY(part.getSizeY());
                    response.setSizeZ(part.getSizeZ());

                    // 메모 조회
                    PartNote note = partNoteMapper.findByPartId(part.getId());
                    if (note != null) {
                        response.setNote(note.getNote());
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    public PartResponse getPartById(Long partId) {
        Part part = partMapper.findById(partId);
        if (part == null) {
            throw new IllegalArgumentException("부품을 찾을 수 없습니다: ID=" + partId);
        }

        PartResponse response = new PartResponse();
        response.setId(part.getId());
        response.setName(part.getName());

        // ✅ displayName 정책 동일 적용
        String resolvedDisplayName =
                (part.getDisplayName() != null && !part.getDisplayName().isBlank())
                        ? part.getDisplayName()
                        : part.getName();
        response.setDisplayName(resolvedDisplayName);

        // ✅ 추가: node 매핑 필드 내려주기
        response.setPartKey(part.getPartKey());
        response.setNodeIndex(part.getNodeIndex());

        response.setPositionX(part.getPositionX());
        response.setPositionY(part.getPositionY());
        response.setPositionZ(part.getPositionZ());
        response.setSizeX(part.getSizeX());
        response.setSizeY(part.getSizeY());
        response.setSizeZ(part.getSizeZ());

        PartNote note = partNoteMapper.findByPartId(partId);
        if (note != null) {
            response.setNote(note.getNote());
        }

        return response;
    }

    @Transactional
    public void saveOrUpdateNote(Long partId, NoteRequest request) {
        PartNote note = new PartNote();
        note.setPartId(partId);
        note.setNote(request.getNote());
        partNoteMapper.insertOrUpdateNote(note);
    }

    @Transactional
    public void deleteNote(Long partId) {
        partNoteMapper.deleteByPartId(partId);
    }

    private CadFileResponse convertToResponse(CadFile cadFile) {
        CadFileResponse response = new CadFileResponse();
        response.setId(cadFile.getId());
        response.setOriginalFilename(cadFile.getOriginalFilename());
        response.setGlbFilePath(cadFile.getGlbFilePath());
        response.setUploadedAt(cadFile.getUploadedAt());
        response.setFileSize(cadFile.getFileSize());
        response.setStatus(cadFile.getStatus());

        log.debug("convertToResponse: ID={}, glbFilePath={}, status={}",
                cadFile.getId(), cadFile.getGlbFilePath(), cadFile.getStatus());

//        // 부품 목록 조회
//        if (cadFile.getId() != null) {
//            response.setParts(getPartsByCadFileId(cadFile.getId()));
//        }

        return response;
    }

    @Transactional
    public void renamePart(Long partId, String displayName) {
        partMapper.updateDisplayNameById(partId, displayName);
    }

    @Transactional
    public void renamePartByKey(Long cadFileId, String partKey, String displayName) {
        partMapper.updateDisplayNameByCadFileIdAndPartKey(cadFileId, partKey, displayName);
    }

}
