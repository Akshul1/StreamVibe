package com.stream.app.service;

import com.stream.app.entity.Video;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoService {

    Video save(Video video, MultipartFile file);

    Video get(String videoId);

    Video getByTitle(String title);

    List<Video> getAll();

    // NEW — returns the file from disk so the controller can stream it
    Resource getVideoAsResource(String videoId);
}
