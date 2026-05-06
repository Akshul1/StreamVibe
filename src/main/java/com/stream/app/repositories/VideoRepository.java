package com.stream.app.repositories;

import com.stream.app.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, String> {

    // Used by getByTitle()
    Optional<Video> findByTitle(String title);
}
