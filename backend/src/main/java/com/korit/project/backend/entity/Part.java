package com.korit.project.backend.entity;

import lombok.Data;

@Data
public class Part {
    private Long id;
    private Long cadFileId;
    private String name;
    private Double positionX;
    private Double positionY;
    private Double positionZ;
    private Double sizeX;
    private Double sizeY;
    private Double sizeZ;
}
