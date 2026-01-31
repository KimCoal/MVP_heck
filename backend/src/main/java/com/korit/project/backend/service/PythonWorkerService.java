package com.korit.project.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korit.project.backend.dto.PartMetadata;
import com.korit.project.backend.entity.Part;
import com.korit.project.backend.mapper.CadFileMapper;
import com.korit.project.backend.mapper.PartMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonWorkerService {

    private final CadFileMapper cadFileMapper;
    private final PartMapper partMapper;
    private final ObjectMapper objectMapper;

    @Value("${python.worker.script-path}")
    private String scriptPath; // 기본: cad_converter.py

    @Value("${python.executable}")
    private String pythonExecutable;

    @Value("${file.upload.converted-dir}")
    private String convertedDir;

    @Value("${freecad.cmd:FreeCADCmd.exe}")
    private String freecadCmd;

    @Async
    public CompletableFuture<Void> processCadFile(Long cadFileId, String inputFilePath, String originalFilename) {
        try {
            log.info("Worker 시작: CAD File ID={}", cadFileId);
            cadFileMapper.updateStatus(cadFileId, "PROCESSING");

            Path inputPath = Paths.get(inputFilePath);
            if (!Files.exists(inputPath)) {
                log.error("입력 파일을 찾을 수 없습니다: {}", inputFilePath);
                processCadFileResult(cadFileId, null, "{\"parts\":[]}", null);
                return CompletableFuture.completedFuture(null);
            }

            String ext = getExt(originalFilename);

            // 스크립트 경로 절대화
            Path scriptAbsolutePath = Paths.get(scriptPath);
            if (!scriptAbsolutePath.isAbsolute()) {
                String userDir = System.getProperty("user.dir");
                scriptAbsolutePath = Paths.get(userDir, scriptPath);
            }

            // ✅ STEP/STP/IGES 분기: FreeCADCmd + step_to_parts.py + parts_to_glb.py
            if (isStepLike(ext)) {
                runStepToPartsThenGlb(cadFileId, inputPath, scriptAbsolutePath);
                return CompletableFuture.completedFuture(null);
            }

            // ✅ 기존 mesh(STL/OBJ/PLY) 분기(유지)
            runMeshToGlbAndMetadata(cadFileId, inputPath, originalFilename, scriptAbsolutePath);
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Worker 실행 중 오류: CAD File ID={}", cadFileId, e);
            try {
                processCadFileResult(cadFileId, null, "{\"parts\":[]}", null);
            } catch (Exception ex) {
                log.error("오류 처리 실패", ex);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * STEP → (1) step_to_parts.py로 STL + parts.json 생성
     *     → (2) parts_to_glb.py로 assembly.glb + node_map.json 생성
     *     → (3) DB 반영 (glb_file_path, parts insert, node_index update)
     */
    private void runStepToPartsThenGlb(Long cadFileId, Path inputPath, Path cadConverterAbsPath) throws Exception {
        Path workerDir = cadConverterAbsPath.getParent();
        Path stepToPartsPath = workerDir.resolve("step_to_parts.py");
        Path partsToGlbPath = workerDir.resolve("parts_to_glb.py");

        // outputDir: temp/parts/{cadFileId}
        Path uploadDir = inputPath.getParent();    // .../temp/upload
        Path tempRoot = uploadDir.getParent();     // .../temp
        Path partsDir = tempRoot.resolve("parts").resolve(String.valueOf(cadFileId));
        Files.createDirectories(partsDir);

        Path partsJson = partsDir.resolve("parts.json");

        // ---- (1) FreeCADCmd: STEP → per-part STL + parts.json ----
        String in = inputPath.toAbsolutePath().toString().replace("\\", "/");
        String outDir = partsDir.toAbsolutePath().toString().replace("\\", "/");
        String jsonPath = partsJson.toAbsolutePath().toString().replace("\\", "/");
        String script = stepToPartsPath.toAbsolutePath().toString().replace("\\", "/");

        String freecadInline =
                "import runpy, sys; " +
                        "sys.argv=[" +
                        "r'step_to_parts.py'," +
                        "r'" + in + "'," +
                        "r'" + outDir + "'," +
                        "r'--format', r'stl'," +
                        "r'--linear', r'10.0'," +
                        "r'--json-out', r'" + jsonPath + "'," +
                        "r'--skip-degenerate'" +
                        "]; " +
                        "runpy.run_path(r'" + script + "', run_name='__main__')";

        log.info("STEP 처리 시작: cadFileId={}, FreeCADCmd={}", cadFileId, freecadCmd);
        log.info("partsDir={}", partsDir.toAbsolutePath());
        log.info("partsJson={}", partsJson.toAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(freecadCmd, "-c", freecadInline);
        pb.redirectErrorStream(true);

        Process p = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) out.append(line).append("\n");
        }

        int exit = p.waitFor();
        if (exit != 0) {
            log.error("FreeCADCmd 실패: exitCode={}, output={}", exit, out);
            processCadFileResult(cadFileId, null, "{\"parts\":[]}", null);
            return;
        }

        if (!Files.exists(partsJson)) {
            log.error("parts.json 생성 실패: {}", partsJson.toAbsolutePath());
            processCadFileResult(cadFileId, null, "{\"parts\":[]}", null);
            return;
        }

        String metadataJson = Files.readString(partsJson, StandardCharsets.UTF_8).trim();
        log.info("STEP parts 생성 완료: cadFileId={}, parts.json size={}", cadFileId, metadataJson.length());

        // ---- (2) parts_to_glb.py: parts.json(+stl들) → assembly.glb + node_map.json ----
        GlbBuildResult glbResult = runPartsToGlb(cadFileId, partsJson, partsToGlbPath);

        // ---- (3) DB 반영 ----
        processCadFileResult(cadFileId,
                glbResult != null ? glbResult.getGlbFilePath() : null,
                metadataJson,
                glbResult != null ? glbResult.getNodeMapPath() : null);
    }

    private GlbBuildResult runPartsToGlb(Long cadFileId, Path partsJson, Path partsToGlbPath) throws Exception {
        if (!Files.exists(partsToGlbPath)) {
            log.warn("parts_to_glb.py가 없습니다. GLB 생성은 건너뜁니다. path={}", partsToGlbPath.toAbsolutePath());
            return null;
        }

        Path convertedPath = resolveConvertedDirAbs();
        Files.createDirectories(convertedPath);

        // cadFileId 기준으로 폴더 분리 추천
        Path cadOutDir = convertedPath.resolve(String.valueOf(cadFileId));
        Files.createDirectories(cadOutDir);

        Path glbOut = cadOutDir.resolve("assembly.glb");
        Path mapOut = cadOutDir.resolve("node_map.json");

        ProcessBuilder pb = new ProcessBuilder(
                pythonExecutable,
                partsToGlbPath.toAbsolutePath().toString(),
                "--parts-json", partsJson.toAbsolutePath().toString(),
                "--out-glb", glbOut.toAbsolutePath().toString(),
                "--out-map", mapOut.toAbsolutePath().toString(),
                "--node-name", "partKey",
                "--write-node-index"
        );

        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) output.append(line).append("\n");
        }

        int exit = p.waitFor();
        if (exit != 0) {
            log.error("parts_to_glb 실패: exitCode={}, output={}", exit, output);
            return null;
        }

        if (!Files.exists(glbOut)) {
            log.error("assembly.glb 생성 실패: {}", glbOut.toAbsolutePath());
            return null;
        }
        if (!Files.exists(mapOut)) {
            log.warn("node_map.json 생성 실패(선택): {}", mapOut.toAbsolutePath());
        }

        log.info("GLB 생성 완료: cadFileId={}, glb={}, map={}",
                cadFileId, glbOut.toAbsolutePath(), mapOut.toAbsolutePath());

        GlbBuildResult r = new GlbBuildResult();
        r.setGlbFilePath(glbOut.toAbsolutePath().toString());
        r.setNodeMapPath(mapOut.toAbsolutePath().toString());
        return r;
    }

    private void runMeshToGlbAndMetadata(Long cadFileId, Path inputPath, String originalFilename, Path scriptAbsolutePath) throws Exception {
        Path convertedPath = resolveConvertedDirAbs();
        Files.createDirectories(convertedPath);

        String glbFileName = cadFileId + "_" + originalFilename.replaceAll("\\.[^.]+$", ".glb");
        String glbFilePath = convertedPath.resolve(glbFileName).toString();

        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                scriptAbsolutePath.toString(),
                inputPath.toAbsolutePath().toString(),
                glbFilePath
        );

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("cad_converter 실패: exitCode={}, output={}", exitCode, output);
            processCadFileResult(cadFileId, null, "{\"parts\":[]}", null);
            return;
        }

        String metadataJson = output.toString().trim();
        processCadFileResult(cadFileId, glbFilePath, metadataJson, null);
    }

    private Path resolveConvertedDirAbs() {
        if (Paths.get(convertedDir).isAbsolute()) return Paths.get(convertedDir);
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, convertedDir);
    }

    private boolean isStepLike(String ext) {
        return ext.equals("step") || ext.equals("stp") || ext.equals("igs") || ext.equals("iges");
    }

    private String getExt(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return "";
        return filename.substring(idx + 1).toLowerCase();
    }

    @Transactional
    private void processCadFileResult(Long cadFileId, String glbFilePath, String metadataJson, String nodeMapPath) {
        try {
            PartMetadata metadata = objectMapper.readValue(metadataJson, PartMetadata.class);

            if (glbFilePath != null) {
                cadFileMapper.updateGlbPath(cadFileId, glbFilePath);
            }

            // parts는 항상 새로 덮어쓰기
            partMapper.deleteByCadFileId(cadFileId);

            if (metadata.getParts() != null && !metadata.getParts().isEmpty()) {
                int fallbackIndex = 0;

                for (PartMetadata.PartInfo partInfo : metadata.getParts()) {
                    Part part = new Part();
                    part.setCadFileId(cadFileId);

                    part.setName(partInfo.getName());
                    part.setDisplayName(null);

                    String partKey = partInfo.getPartKey();
                    if (partKey == null || partKey.isBlank()) {
                        fallbackIndex++;
                        String base = (partInfo.getName() == null || partInfo.getName().isBlank())
                                ? "Part"
                                : partInfo.getName().trim();
                        partKey = "fallback:" + base + ":" + fallbackIndex;
                    }
                    part.setPartKey(partKey);

                    // nodeIndex는 GLB 단계에서 node_map.json으로 업데이트될 수 있음
                    part.setNodeIndex(partInfo.getNodeIndex());
                    part.setNodePath(partInfo.getNodePath());

                    // 지금은 트리 미사용 (필요 시 추후 2-pass로 parent_part_id 채우기)
                    part.setParentPartId(null);

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

            // ✅ node_map.json이 있으면 node_index를 partKey 기준으로 업데이트(가능한 것만)
            if (nodeMapPath != null && !nodeMapPath.isBlank()) {
                tryUpdateNodeIndexFromNodeMap(cadFileId, nodeMapPath);
            }

            cadFileMapper.updateStatus(cadFileId, "COMPLETED");
            log.info("CAD 파일 처리 완료: ID={}", cadFileId);

        } catch (Exception e) {
            log.error("CAD 파일 처리 결과 저장 실패: ID={}", cadFileId, e);
            cadFileMapper.updateStatus(cadFileId, "FAILED");
            throw new RuntimeException("파일 처리 결과 저장 실패", e);
        }
    }

    private void tryUpdateNodeIndexFromNodeMap(Long cadFileId, String nodeMapPath) {
        try {
            Path p = Paths.get(nodeMapPath);
            if (!Files.exists(p)) {
                log.warn("node_map.json 파일이 없습니다. node_index 업데이트 생략. path={}", nodeMapPath);
                return;
            }
            String json = Files.readString(p, StandardCharsets.UTF_8);
            List<NodeMapItem> items = objectMapper.readValue(json, new TypeReference<List<NodeMapItem>>() {});
            if (items == null || items.isEmpty()) return;

            int updated = 0;
            for (NodeMapItem item : items) {
                if (item.getPartKey() == null || item.getPartKey().isBlank()) continue;
                if (item.getNodeIndex() == null) continue; // nodeIndex가 없으면 생략(이름 매핑만 활용 가능)
                updated += partMapper.updateNodeIndexByCadFileIdAndPartKey(cadFileId, item.getPartKey(), item.getNodeIndex());
            }
            log.info("node_index 업데이트 완료: cadFileId={}, updatedRows={}", cadFileId, updated);

        } catch (Exception e) {
            // node_index는 부가정보라 실패해도 전체를 FAILED로 만들 필요는 없음(MVP)
            log.warn("node_index 업데이트 실패(무시): cadFileId={}, err={}", cadFileId, e.toString());
        }
    }

    @Data
    private static class GlbBuildResult {
        private String glbFilePath;
        private String nodeMapPath;
    }

    @Data
    private static class NodeMapItem {
        private String partKey;
        private String nodeName;
        private String nodePath;
        private String parentKey;
        private Integer nodeIndex;
        private String meshPath;
        private Double[] position;
        private Double[] size;
    }
}
