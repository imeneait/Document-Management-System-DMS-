package com.example.storage.controller;

import com.example.storage.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    private String getMockedEmailFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (token.equals("admin-token")) return "admin@example.com";
            if (token.equals("eng-token")) return "user1@eng.com";
            if (token.equals("hr-token")) return "user2@hr.com";
        }
        return null;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("folder") String folder,
                                        HttpServletRequest request) {
        String userEmail = getMockedEmailFromToken(request);
        if (userEmail == null || !storageService.hasAccessToDepartment(userEmail, folder)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }

        try {
            storageService.uploadFile(folder, file);
            return ResponseEntity.ok("File uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/download/{folder}/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String folder,
                                          @PathVariable String filename,
                                          HttpServletRequest request) {
        String userEmail = getMockedEmailFromToken(request);
        if (userEmail == null || !storageService.hasAccessToDepartment(userEmail, folder)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }

        try {
            byte[] data = storageService.downloadFile(folder, filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Download failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{folder}/{filename}")
    public ResponseEntity<?> deleteFile(@PathVariable String folder,
                                        @PathVariable String filename,
                                        HttpServletRequest request) {
        String userEmail = getMockedEmailFromToken(request);
        if (userEmail == null || !storageService.hasAccessToDepartment(userEmail, folder)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }

        try {
            storageService.deleteFile(folder, filename);
            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Delete failed: " + e.getMessage());
        }
    }
}
