package com.cloudide.cloudide.controller;

import com.cloudide.cloudide.dto.FileRequest;
import com.cloudide.cloudide.dto.FileResponse;
import com.cloudide.cloudide.service.FileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> getFilesByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(fileService.getFilesByProjectId(projectId));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileResponse> getFileById(
            @PathVariable Long projectId,
            @PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.getFileById(projectId, fileId));
    }

    @PostMapping
    public ResponseEntity<FileResponse> createFile(
            @PathVariable Long projectId,
            @Valid @RequestBody FileRequest request) {
        FileResponse response = fileService.createFile(projectId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{fileId}")
    public ResponseEntity<FileResponse> updateFile(
            @PathVariable Long projectId,
            @PathVariable Long fileId,
            @Valid @RequestBody FileRequest request) {
        return ResponseEntity.ok(fileService.updateFile(projectId, fileId, request));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long projectId,
            @PathVariable Long fileId) {
        fileService.deleteFile(projectId, fileId);
        return ResponseEntity.noContent().build();
    }
}
