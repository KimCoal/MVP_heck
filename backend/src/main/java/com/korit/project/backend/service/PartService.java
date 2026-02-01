package com.korit.project.backend.service;

import com.korit.project.backend.dto.NoteRequest;
import com.korit.project.backend.dto.PartResponse;
import com.korit.project.backend.dto.resp.ApiRespDto;
import com.korit.project.backend.entity.Part;
import com.korit.project.backend.entity.PartNote;
import com.korit.project.backend.mapper.PartMapper;
import com.korit.project.backend.mapper.PartNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartService {

    private final PartMapper partMapper;
    private final PartNoteMapper partNoteMapper;

    public ApiRespDto<PartResponse> getPartById(Long partId) {
        Part part = partMapper.findById(partId);
        if (part == null) {
            return new ApiRespDto<>("failed", "부품을 찾을 수 없습니다: ID=" + partId, null);
        }

        PartResponse response = convertToResponse(part);
        return new ApiRespDto<>("success", "부품 조회가 완료되었습니다.", response);
    }

    public ApiRespDto<List<PartResponse>> getPartsByCadFileId(Long cadFileId) {
        List<Part> parts = partMapper.findByCadFileId(cadFileId);
        List<PartResponse> responses = parts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return new ApiRespDto<>("success", "부품 목록 조회가 완료되었습니다.", responses);
    }

    @Transactional
    public ApiRespDto<Void> saveOrUpdateNote(Long partId, NoteRequest request) {
        PartNote note = new PartNote();
        note.setPartId(partId);
        note.setNote(request.getNote());
        partNoteMapper.insertOrUpdateNote(note);
        return new ApiRespDto<>("success", "메모가 저장되었습니다.", null);
    }

    @Transactional
    public ApiRespDto<Void> deleteNote(Long partId) {
        partNoteMapper.deleteByPartId(partId);
        return new ApiRespDto<>("success", "메모가 삭제되었습니다.", null);
    }

    @Transactional
    public ApiRespDto<Void> renamePart(Long partId, String displayName) {
        partMapper.updateDisplayNameById(partId, displayName);
        return new ApiRespDto<>("success", "부품 이름이 변경되었습니다.", null);
    }

    private PartResponse convertToResponse(Part part) {
        PartResponse response = new PartResponse();
        response.setId(part.getId());
        response.setName(part.getName());

        String resolvedDisplayName =
                (part.getDisplayName() != null && !part.getDisplayName().isBlank())
                        ? part.getDisplayName()
                        : part.getName();
        response.setDisplayName(resolvedDisplayName);

        response.setPartKey(part.getPartKey());
        response.setNodeIndex(part.getNodeIndex());

        response.setPositionX(part.getPositionX());
        response.setPositionY(part.getPositionY());
        response.setPositionZ(part.getPositionZ());
        response.setSizeX(part.getSizeX());
        response.setSizeY(part.getSizeY());
        response.setSizeZ(part.getSizeZ());

        PartNote note = partNoteMapper.findByPartId(part.getId());
        if (note != null) {
            response.setNote(note.getNote());
        }

        return response;
    }
}
