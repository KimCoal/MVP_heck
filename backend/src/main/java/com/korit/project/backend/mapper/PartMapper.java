package com.korit.project.backend.mapper;

import com.korit.project.backend.entity.Part;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PartMapper {
    void insertPart(Part part);
    Part findById(Long id);
    List<Part> findByCadFileId(Long cadFileId);
}
