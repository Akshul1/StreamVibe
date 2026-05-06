package com.stream.app.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "videos")
public class Video {
    @Id
    @Column(name = "video_id")
    private String videoId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "contentType")
    private String contentType;

    @Column(columnDefinition = "text")
    private String meta;   // JSON / metadata

    @Column(name = "file_path", nullable = false)
    private String filePath;

}
