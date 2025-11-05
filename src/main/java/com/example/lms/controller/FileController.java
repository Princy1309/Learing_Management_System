package com.example.lms.controller;

import com.example.lms.service.FileService;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            // Log who called the endpoint (for debugging)
            if (principal != null) {
                System.out.println("[FileController] upload called by: " + principal.getName());
            } else {
                System.out.println("[FileController] upload called by: <anonymous>");
            }

            String url = fileService.uploadFile(file);
            return ResponseEntity.ok().body("✅ File uploaded successfully! URL: " + url);
        } catch (Exception e) {
            // log stacktrace and return the message so UI can show it
            e.printStackTrace();
            return ResponseEntity.status(400).body("❌ Upload failed: " + e.getMessage());
        }
    }

}
