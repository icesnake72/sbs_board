package com.sbs.board.global.entity;

import com.sbs.board.post.dto.PostImageResponse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;

@Entity
@Table(name="post_images")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private String storedName;

    private String originalName;
    private String contentType;
    private long size;

    @Column(nullable = false)
    private int sortOrder;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();


    private static final String URL_PREFIX = "/images/";

    public void assignPost(Post post) {
        this.post = post;
    }

    public static PostImageResponse toDto(PostImage image) {
        PostImageResponse dto = new PostImageResponse();
        dto.setId(image.getId());
        dto.setUrl(URL_PREFIX + image.getStoredName());
        dto.setOriginalName(image.getOriginalName());
        dto.setSortOrder(image.getSortOrder());

        return dto;
    }
}
