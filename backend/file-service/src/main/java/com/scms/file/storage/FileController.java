package com.scms.file.storage;

import com.scms.file.common.ApiResponse;
import com.scms.file.common.RequestIdFilter;
import com.scms.file.storage.dto.FileDownloadUrlResponse;
import com.scms.file.storage.dto.FileUploadResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String objectKey = fileStorageService.upload(file);
        return ApiResponse.success(new FileUploadResponse(objectKey), requestId());
    }

    @GetMapping("/{objectKey}/download-url")
    public ApiResponse<FileDownloadUrlResponse> getDownloadUrl(@PathVariable String objectKey) {
        String url = fileStorageService.presignedDownloadUrl(objectKey, 3600);
        return ApiResponse.success(new FileDownloadUrlResponse(objectKey, url), requestId());
    }

    @GetMapping("/{objectKey}")
    public void download(@PathVariable String objectKey, HttpServletResponse response) {
        try (InputStream inputStream = fileStorageService.download(objectKey)) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectKey + "\"");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            inputStream.transferTo(response.getOutputStream());
        } catch (Exception exception) {
            throw new IllegalStateException("download failed", exception);
        }
    }

    private String requestId() {
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID);
        return requestId == null ? "N/A" : requestId;
    }
}
