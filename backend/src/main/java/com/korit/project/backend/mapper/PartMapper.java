package com.korit.project.backend.mapper;

import com.korit.project.backend.entity.Part;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PartMapper {
    void insertPart(Part part);

    int deleteByCadFileId(Long cadFileId);

    Part findById(Long id);

    List<Part> findByCadFileId(Long cadFileId);

    void updateDisplayNameById(Long id, String displayName);

    void updateDisplayNameByCadFileIdAndPartKey(Long cadFileId, String partKey, String displayName);

    int updateNodeIndexByCadFileIdAndPartKey(Long cadFileId, String partKey, Integer nodeIndex);

}
