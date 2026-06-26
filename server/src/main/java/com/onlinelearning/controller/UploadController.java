package com.onlinelearning.controller;

import com.onlinelearning.common.ApiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class UploadController {
    @Value("${app.upload-root}")
    private String uploadRoot;

    @PostMapping("/upload")
    public ApiResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "module", defaultValue = "materials") String module) throws IOException {
        if (file.isEmpty()) {
            return ApiResult.fail("上传文件不能为空");
        }
        String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf(".")) : "";
        String cleanModule = module.replaceAll("[^a-zA-Z0-9_-]", "");
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        File dir = new File(uploadRoot, cleanModule);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = new File(dir, filename);
        file.transferTo(target);
        String relative = "/upload/" + cleanModule + "/" + filename;
        Map<String, String> result = new HashMap<String, String>();
        result.put("url", relative);
        result.put("absolutePath", target.getAbsolutePath());
        result.put("name", original);
        return ApiResult.ok(result);
    }
}
