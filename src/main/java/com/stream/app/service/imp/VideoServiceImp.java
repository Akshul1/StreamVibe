package com.stream.app.service.imp;

import com.stream.app.entity.Video;
import com.stream.app.repositories.VideoRepository;
import com.stream.app.service.VideoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class VideoServiceImp implements VideoService {

    private final VideoRepository videoRepository;

    @Value("${files.video}")
    private String DIR;

    public VideoServiceImp(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    // ── Create the storage folder on startup ─────────────────────────────────
    @PostConstruct
    public void init() {
        File folder = new File(DIR);
        if (!folder.exists()) {
            folder.mkdirs();
            System.out.println("Storage folder created: " + DIR);
        } else {
            System.out.println("Storage folder already exists: " + DIR);
        }
    }

    // ── Upload & save ─────────────────────────────────────────────────────────
    @Override
    public Video save(Video video, MultipartFile file) {
        try {
            String filename      = StringUtils.cleanPath(file.getOriginalFilename());
            String contentType   = file.getContentType();
            InputStream inputStream = file.getInputStream();

            Path targetPath = Paths.get(StringUtils.cleanPath(DIR)).resolve(filename);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            video.setContentType(contentType);
            video.setFilePath(targetPath.toString());

            return videoRepository.save(video);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Get one video's metadata ──────────────────────────────────────────────
    @Override
    public Video get(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + videoId));
    }

    // ── Get one video's metadata by title ────────────────────────────────────
    @Override
    public Video getByTitle(String title) {
        return videoRepository.findByTitle(title)
                .orElseThrow(() -> new RuntimeException("Video not found with title: " + title));
    }

    // ── List all videos ───────────────────────────────────────────────────────
    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    // ── Load the actual file from disk (used for streaming) ───────────────────
    @Override
    public Resource getVideoAsResource(String videoId) {
        Video video = get(videoId);
        Path filePath = Paths.get(video.getFilePath());
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            throw new RuntimeException("Video file not found on disk for id: " + videoId);
        }
        return resource;
    }
}
