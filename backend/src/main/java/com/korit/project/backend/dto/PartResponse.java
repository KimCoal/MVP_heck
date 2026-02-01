package com.korit.project.backend.dto;

import lombok.Data;

@Data
public class PartResponse {
    private Long id;
    private String name;
    private String displayName;

    // ✅ 추가: GLB 노드 매핑용 키
    private String partKey;
    private Integer nodeIndex;

    private Double positionX;
    private Double positionY;
    private Double positionZ;
    private Double sizeX;
    private Double sizeY;
    private Double sizeZ;

    private String note;
}
