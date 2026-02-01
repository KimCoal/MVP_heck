package com.korit.project.backend.mapper;

import com.korit.project.backend.entity.Part;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PartMapper {
    void insertPart(Part part);

    int deleteByCadFileId(@Param("cadFileId") Long cadFileId);

    Part findById(@Param("id") Long id);

    List<Part> findByCadFileId(@Param("cadFileId") Long cadFileId);

    void updateDisplayNameById(
            @Param("id") Long id,
            @Param("displayName") String displayName
    );

    void updateDisplayNameByCadFileIdAndPartKey(
            @Param("cadFileId") Long cadFileId,
            @Param("partKey") String partKey,
            @Param("displayName") String displayName
    );

    int updateNodeIndexByCadFileIdAndPartKey(
            @Param("cadFileId") Long cadFileId,
            @Param("partKey") String partKey,
            @Param("nodeIndex") Integer nodeIndex
    );
}
