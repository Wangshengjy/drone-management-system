package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(MultipartFile file) throws Exception {
        SecurityUtils.requireUser();
        if (file == null || file.isEmpty()) {
            throw new com.drone.collab.common.BusinessException("文件为空");
        }
        String orig = file.getOriginalFilename();
        String ext = "";
        if (orig != null && orig.contains(".")) {
            ext = orig.substring(orig.lastIndexOf('.'));
        }
        String name = UUID.randomUUID() + ext;
        Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(name);
        file.transferTo(target.toFile());
        String url = "/uploads/" + name;
        return ApiResponse.ok(Map.of("url", url));
    }
}
