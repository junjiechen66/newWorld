package com.newworld.controller;

import com.newworld.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "文件上传", description = "图片等文件上传接口")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    private File uploadDir;

    @PostConstruct
    public void init() {
        Path path = Paths.get(uploadPath).toAbsolutePath().normalize();
        uploadDir = path.toFile();
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    @Operation(summary = "上传文件")
    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过5MB");
        }

        // 校验文件类型
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_TYPES.contains(ext)) {
            return Result.error("仅支持 jpg/jpeg/png/gif/webp 格式图片");
        }

        // 生成唯一文件名
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // 保存文件到绝对路径
        try {
            File dest = new File(uploadDir, newFileName);
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }

        Map<String, String> result = new HashMap<>();
        result.put("url", "/api/uploads/" + newFileName);
        return Result.success(result);
    }
}
