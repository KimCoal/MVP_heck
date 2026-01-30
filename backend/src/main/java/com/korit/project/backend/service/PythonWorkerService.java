package com.korit.project.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.korit.project.backend.dto.PartMetadata;
import com.korit.project.backend.entity.Part;
import com.korit.project.backend.mapper.CadFileMapper;
import com.korit.project.backend.mapper.PartMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonWorkerService {

    private final CadFileMapper cadFileMapper;
    private final PartMapper partMapper;
    private final ObjectMapper objectMapper;

    @Value("${python.worker.script-path}")
    private String scriptPath;

    @Value("${python.executable}")
    private String pythonExecutable;

    @Value("${file.upload.converted-dir}")
    private String convertedDir;

    @Async
    public CompletableFuture<Void> processCadFile(Long cadFileId, String inputFilePath, String originalFilename) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Python Worker 시작: CAD File ID={}", cadFileId);
                
                // 입력 파일 존재 확인
                Path inputPath = Paths.get(inputFilePath);
                if (!Files.exists(inputPath)) {
                    log.error("입력 파일을 찾을 수 없습니다: {}", inputFilePath);
                    processCadFileResult(cadFileId, null, "{\"parts\":[]}");
                    return;
                }
                log.info("입력 파일 확인: {}", inputPath.toAbsolutePath());
                
                // 출력 디렉토리 생성 (절대 경로로 변환)
                Path convertedPath;
                if (Paths.get(convertedDir).isAbsolute()) {
                    convertedPath = Paths.get(convertedDir);
                } else {
                    String userDir = System.getProperty("user.dir");
                    convertedPath = Paths.get(userDir, convertedDir);
                }
                Files.createDirectories(convertedPath);
                log.info("출력 디렉토리: {}", convertedPath.toAbsolutePath());
                
                // GLB 파일 경로 생성
                String glbFileName = cadFileId + "_" + originalFilename.replaceAll("\\.[^.]+$", ".glb");
                String glbFilePath = convertedPath.resolve(glbFileName).toString();
                
                // Python 스크립트 경로를 절대 경로로 변환
                Path scriptAbsolutePath = Paths.get(scriptPath);
                if (!scriptAbsolutePath.isAbsolute()) {
                    // 상대 경로인 경우 현재 작업 디렉토리 기준으로 변환
                    String userDir = System.getProperty("user.dir");
                    scriptAbsolutePath = Paths.get(userDir, scriptPath);
                }
                
                log.info("Python 스크립트 경로: {}", scriptAbsolutePath);
                log.info("입력 파일: {}", inputFilePath);
                log.info("출력 파일: {}", glbFilePath);
                
                // Python 스크립트 실행
                ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    scriptAbsolutePath.toString(),
                    inputFilePath,
                    glbFilePath
                );
                
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                
                // 출력 읽기
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                
                int exitCode = process.waitFor();
                
                if (exitCode != 0) {
                    log.error("Python Worker 실패: exitCode={}, output={}", exitCode, output);
                    processCadFileResult(cadFileId, null, "{\"parts\":[]}");
                    return;
                }
                
                // 메타데이터 파싱 및 저장
                String metadataJson = output.toString().trim();
                log.info("Python Worker 완료: CAD File ID={}, Metadata={}", cadFileId, metadataJson);
                
                processCadFileResult(cadFileId, glbFilePath, metadataJson);
                
            } catch (Exception e) {
                log.error("Python Worker 실행 중 오류: CAD File ID={}", cadFileId, e);
                try {
                    processCadFileResult(cadFileId, null, "{\"parts\":[]}");
                } catch (Exception ex) {
                    log.error("오류 처리 실패", ex);
                }
            }
        });
    }

    @Transactional
    private void processCadFileResult(Long cadFileId, String glbFilePath, String metadataJson) {
        try {
            // 메타데이터 파싱
            PartMetadata metadata = objectMapper.readValue(metadataJson, PartMetadata.class);

            // GLB 파일 경로 업데이트
            if (glbFilePath != null) {
                cadFileMapper.updateGlbPath(cadFileId, glbFilePath);
            }

            // 부품 정보 저장
            if (metadata.getParts() != null && !metadata.getParts().isEmpty()) {
                for (PartMetadata.PartInfo partInfo : metadata.getParts()) {
                    Part part = new Part();
                    part.setCadFileId(cadFileId);
                    part.setName(partInfo.getName());
                    
                    if (partInfo.getPosition() != null && partInfo.getPosition().length >= 3) {
                        part.setPositionX(partInfo.getPosition()[0]);
                        part.setPositionY(partInfo.getPosition()[1]);
                        part.setPositionZ(partInfo.getPosition()[2]);
                    }
                    
                    if (partInfo.getSize() != null && partInfo.getSize().length >= 3) {
                        part.setSizeX(partInfo.getSize()[0]);
                        part.setSizeY(partInfo.getSize()[1]);
                        part.setSizeZ(partInfo.getSize()[2]);
                    }
                    
                    partMapper.insertPart(part);
                }
            }

            // 상태 업데이트
            cadFileMapper.updateStatus(cadFileId, "COMPLETED");
            log.info("CAD 파일 처리 완료: ID={}", cadFileId);

        } catch (Exception e) {
            log.error("CAD 파일 처리 결과 저장 실패: ID={}", cadFileId, e);
            cadFileMapper.updateStatus(cadFileId, "FAILED");
            throw new RuntimeException("파일 처리 결과 저장 실패", e);
        }
    }
}
