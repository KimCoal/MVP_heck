package com.korit.project.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class PartMetadata {
    private List<PartInfo> parts;
    
    @Data
    public static class PartInfo {
        private String name;
        private Double[] position;
        private Double[] size;
    }
}
