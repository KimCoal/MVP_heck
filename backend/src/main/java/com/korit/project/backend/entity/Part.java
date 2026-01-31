package com.korit.project.backend.entity;

import lombok.Data;

@Data
public class Part {
    private Long id;
    private Long cadFileId;

    private String name;
    private String displayName;

    // ✅ B안 핵심 식별자/노드 정보
    private String partKey;
    private Integer nodeIndex;
    private String nodePath;
    private Long parentPartId;

    private Double positionX;
    private Double positionY;
    private Double positionZ;

    private Double sizeX;
    private Double sizeY;
    private Double sizeZ;
}
