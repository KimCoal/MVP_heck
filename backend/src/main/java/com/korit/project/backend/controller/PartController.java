package com.korit.project.backend.controller;

import com.korit.project.backend.dto.NoteRequest;
import com.korit.project.backend.dto.PartRenameRequest;
import com.korit.project.backend.dto.PartResponse;
import com.korit.project.backend.service.CadFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartController {

    private final CadFileService cadFileService;

    @GetMapping("/{id}")
    public ResponseEntity<PartResponse> getPartById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(cadFileService.getPartById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/note")
    public ResponseEntity<Void> saveOrUpdateNote(@PathVariable Long id, @RequestBody NoteRequest request) {
        try {
            cadFileService.saveOrUpdateNote(id, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/note")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        try {
            cadFileService.deleteNote(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/display-name")
    public ResponseEntity<Void> renamePart(@PathVariable Long id,
                                           @RequestBody PartRenameRequest request) {
        if (request == null || request.getDisplayName() == null) {
            return ResponseEntity.badRequest().build();
        }
        cadFileService.renamePart(id, request.getDisplayName());
        return ResponseEntity.ok().build();
    }

}
