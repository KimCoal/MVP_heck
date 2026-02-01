package com.korit.project.backend.service;

import com.korit.project.backend.dto.CadFileResponse;
import com.korit.project.backend.dto.PartResponse;
import com.korit.project.backend.dto.resp.ApiRespDto;
import com.korit.project.backend.entity.CadFile;
import com.korit.project.backend.mapper.CadFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
    private final PythonWorkerService pythonWorkerService;
    private final PartService partService;

    @Value("${file.upload.temp-dir}")
    private String tempDir;

    @Value("${file.upload.converted-dir}")
    private String convertedDir;

    @Transactional
    public ApiRespDto<CadFileResponse> uploadCadFile(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return new ApiRespDto<>("failed", "파일이 비어있습니다.", null);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return new ApiRespDto<>("failed", "파일명이 없습니다.", null);
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            boolean allowed = extension.equals("stl") || extension.equals("obj") || extension.equals("ply") ||
                    extension.equals("step") || extension.equals("stp") || extension.equals("igs") || extension.equals("iges");

            if (!allowed) {
                return new ApiRespDto<>("failed", "STL, OBJ, PLY, STEP, STP, IGES 파일만 업로드 가능합니다.", null);
            }

            Path tempPath = resolvePath(tempDir);
            Files.createDirectories(tempPath);

            String tempFileName = System.currentTimeMillis() + "_" + originalFilename;
            Path tempFilePath = tempPath.resolve(tempFileName);
            file.transferTo(tempFilePath.toFile());

            if (!Files.exists(tempFilePath)) {
                return new ApiRespDto<>("failed", "파일 저장에 실패했습니다.", null);
            }

            CadFile cadFile = new CadFile();
            cadFile.setOriginalFilename(originalFilename);
            cadFile.setFileSize(file.getSize());
            cadFile.setStatus("UPLOADING");
            cadFileMapper.insertCadFile(cadFile);

            Long cadFileId = cadFile.getId();
            pythonWorkerService.processCadFile(cadFileId, tempFilePath.toAbsolutePath().toString(), originalFilename);

            CadFileResponse response = convertToResponse(cadFile);
            return new ApiRespDto<>("success", "파일 업로드가 완료되었습니다.", response);
        } catch (Exception e) {
            log.error("파일 업로드 실패", e);
            return new ApiRespDto<>("failed", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage(), null);
        }
    }

    public ApiRespDto<List<CadFileResponse>> getAllCadFiles() {
        List<CadFileResponse> files = cadFileMapper.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return new ApiRespDto<>("success", "파일 목록 조회가 완료되었습니다.", files);
    }

    public ApiRespDto<CadFileResponse> getCadFileById(Long id) {
        CadFile cadFile = cadFileMapper.findById(id);
        if (cadFile == null) {
            return new ApiRespDto<>("failed", "파일을 찾을 수 없습니다: ID=" + id, null);
        }

        CadFileResponse response = convertToResponse(cadFile);
        ApiRespDto<List<PartResponse>> partsResult = partService.getPartsByCadFileId(id);
        if ("success".equals(partsResult.getStatus()) && partsResult.getData() != null) {
            response.setParts(partsResult.getData());
        }

        return new ApiRespDto<>("success", "파일 조회가 완료되었습니다.", response);
    }

    public File getGlbFile(Long id) {
        CadFile cadFile = cadFileMapper.findById(id);
        if (cadFile == null || cadFile.getGlbFilePath() == null) {
            throw new IllegalArgumentException("GLB 파일을 찾을 수 없습니다: ID=" + id);
        }
        return new File(cadFile.getGlbFilePath());
    }

    private Path resolvePath(String dir) {
        if (Paths.get(dir).isAbsolute()) {
            return Paths.get(dir);
        }
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, dir);
    }

    private CadFileResponse convertToResponse(CadFile cadFile) {
        CadFileResponse response = new CadFileResponse();
        response.setId(cadFile.getId());
        response.setOriginalFilename(cadFile.getOriginalFilename());
        response.setGlbFilePath(cadFile.getGlbFilePath());
        response.setUploadedAt(cadFile.getUploadedAt());
        response.setFileSize(cadFile.getFileSize());
        response.setStatus(cadFile.getStatus());
        return response;
    }

}
