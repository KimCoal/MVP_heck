package com.korit.project.backend.mapper;

import com.korit.project.backend.entity.PartNote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PartNoteMapper {
    void insertOrUpdateNote(PartNote partNote);
    PartNote findByPartId(Long partId);
    void deleteByPartId(Long partId);
}
