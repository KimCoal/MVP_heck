package com.korit.project.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PartNote {
    private Long id;
    private Long partId;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
