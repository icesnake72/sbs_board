package com.sbs.board.post;

import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "gif", "webp");

    private final Path uploadPath;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadPath);
            log.debug("파일 업로드 디렉토리 준비됨: {}", uploadPath);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_FILE_UPLOAD_DIR);
        }
    }

    public String store(MultipartFile file) {
        if (file==null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        String contentType = file.getContentType();
        if (contentType==null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        // XXXXXXXXXXXXXXXXXXXXX.png
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = uploadPath.resolve(storedName).normalize();
        if( !target.startsWith(uploadPath) ) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        try(InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);    // Overwrite Option으로 파일 복사
            return storedName;
        } catch (IOException ex) {
            log.error("파일 복사 에러: 파일명({}), {}", storedName, ex.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void delete(String fileName) {
        if ( fileName == null || fileName.isBlank() ) {
            return;
        }

        try {
            Path target = uploadPath.resolve(fileName).normalize();
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.error("파일 삭제중 에러가 발생함: {}", ex.getMessage());
        }
    }

    public String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        int dot = originalFilename.lastIndexOf(".");
        if (dot < 0 || dot==originalFilename.length()-1) {
            return "";
        }

        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
