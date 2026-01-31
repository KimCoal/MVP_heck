package com.korit.project.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartMetadata {
    private List<PartInfo> parts;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PartInfo {
        private String partKey;
        private String name;
        private Integer nodeIndex;
        private String nodePath;
        private String parentKey;

        private Double[] position;
        private Double[] size;

        // ✅ 선택: 있어도 되고 없어도 됨
        // - 지금은 DB에 저장 안 해도 됨
        // - 하지만 디버깅/확장 대비로 DTO에만 받아두면 편함
        private String meshPath;
    }
}
