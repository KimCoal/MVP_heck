package com.korit.project.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CadFileResponse {
    private Long id;
    private String originalFilename;
    private String glbFilePath;
    private LocalDateTime uploadedAt;
    private Long fileSize;
    private String status;
    private List<PartResponse> parts;
}
