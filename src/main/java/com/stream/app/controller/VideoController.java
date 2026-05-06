package com.stream.app.controller;

import com.stream.app.entity.Video;
import com.stream.app.payload.CustomMessage;
import com.stream.app.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/video")
@CrossOrigin("*")
@Tag(name = "Videos", description = "Upload, list, and stream videos")
public class VideoController {

    // Chunk size for streaming: 1 MB
    private static final long CHUNK_SIZE = 1024 * 1024;

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    // ─── 1. Upload a video ────────────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a video", description = "Upload a video file along with its title and description")
    public ResponseEntity<?> create(
            @RequestParam("file")        MultipartFile file,
            @RequestParam("title")       String title,
            @RequestParam("description") String description) {

        Video video = new Video();
        video.setVideoId(UUID.randomUUID().toString());
        video.setTitle(title);
        video.setDescription(description);

        Video saved = videoService.save(video, file);

        if (saved != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomMessage.builder()
                        .message("Video could not be uploaded.")
                        .sucess(false)
                        .build());
    }

    // ─── 2. List all videos ───────────────────────────────────────────────────
    @GetMapping
    @Operation(summary = "List all videos", description = "Returns metadata for every video stored in the database")
    public ResponseEntity<List<Video>> getAll() {
        return ResponseEntity.ok(videoService.getAll());
    }

    // ─── 3. Get one video's metadata ─────────────────────────────────────────
    @GetMapping("/{videoId}")
    @Operation(summary = "Get video metadata", description = "Returns metadata for a single video by its ID")
    public ResponseEntity<Video> get(
            @Parameter(description = "UUID of the video") @PathVariable String videoId) {
        return ResponseEntity.ok(videoService.get(videoId));
    }

    // ─── 4. Stream a video (with Range / seek support) ───────────────────────
    /**
     * How streaming works in simple words:
     *
     * A normal download sends the WHOLE file at once.
     * But a video player needs to SEEK (jump to any point).
     * So instead, the player says "give me bytes 0 to 1048575" (first 1 MB).
     * When you seek, it says "give me bytes 5000000 to 6048575".
     * The server sends ONLY those bytes → 206 Partial Content.
     * This is the HTTP Range standard (RFC 7233) used by YouTube, Netflix, etc.
     */
    @GetMapping("/stream/{videoId}")
    @Operation(summary = "Stream a video",
               description = "Streams video bytes. Supports HTTP Range requests so the player can seek to any position.")
    public ResponseEntity<byte[]> stream(
            @Parameter(description = "UUID of the video") @PathVariable String videoId,
            @Parameter(description = "Byte range, e.g. bytes=0-1048575 (optional)")
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        Resource resource = videoService.getVideoAsResource(videoId);
        Video   video     = videoService.get(videoId);

        long fileSize;
        try {
            fileSize = resource.contentLength();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // ── No Range header: send the full file ──────────────────────────────
        if (rangeHeader == null || rangeHeader.isBlank()) {
            try (InputStream is = resource.getInputStream()) {
                byte[] data = is.readAllBytes();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(video.getContentType()))
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                        .body(data);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        // ── Parse Range: bytes=start-end ─────────────────────────────────────
        long[] range = parseRange(rangeHeader, fileSize);
        if (range == null) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        long start  = range[0];
        long end    = range[1];
        long length = end - start + 1;

        try (InputStream is = resource.getInputStream()) {
            is.skip(start);
            byte[] data = is.readNBytes((int) length);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(video.getContentType()))
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(length))
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── Private: parse the Range header ─────────────────────────────────────
    private long[] parseRange(String rangeHeader, long fileSize) {
        try {
            String value = rangeHeader.replace("bytes=", "").trim();
            String[] parts = value.split("-");

            long start = parts[0].isBlank() ? 0 : Long.parseLong(parts[0].trim());
            long end;

            if (parts.length < 2 || parts[1].isBlank()) {
                end = Math.min(start + CHUNK_SIZE - 1, fileSize - 1);
            } else {
                end = Long.parseLong(parts[1].trim());
            }

            end = Math.min(end, fileSize - 1);

            if (start > end || start < 0 || start >= fileSize) return null;
            return new long[]{start, end};

        } catch (NumberFormatException e) {
            return null;
        }
    }
}
