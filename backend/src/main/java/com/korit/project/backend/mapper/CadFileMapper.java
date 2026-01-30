package com.korit.project.backend.mapper;

import com.korit.project.backend.entity.CadFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CadFileMapper {
    void insertCadFile(CadFile cadFile);
    CadFile findById(Long id);
    List<CadFile> findAll();
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    void updateGlbPath(@Param("id") Long id, @Param("glbFilePath") String glbFilePath);
}
