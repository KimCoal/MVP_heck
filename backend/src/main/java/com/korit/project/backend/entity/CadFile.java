package com.korit.project.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CadFile {
    private Long id;
    private String originalFilename;
    private String glbFilePath;
    private LocalDateTime uploadedAt;
    private Long fileSize;
    private String status; // UPLOADING, PROCESSING, COMPLETED, FAILED
}
