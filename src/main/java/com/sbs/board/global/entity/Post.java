package com.sbs.board.global.entity;

import com.sbs.board.post.dto.PostDTO;
import com.sbs.board.post.dto.PostImageResponse;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")   // sortOrder 필드를 기준으로 오름차순 정렬
    @BatchSize(size=100)
    private List<PostImage> images = new ArrayList<>();

    @Column(nullable = false)
    private long viewCount;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static PostDTO toDTO(Post post, Long loginUserId) {
        boolean owner = loginUserId != null && post.isAuthor(loginUserId);

        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setAuthor(post.getAuthor().getNickName());
        dto.setBoard(post.getBoard().getName());
        dto.setBody(post.getBody());
        dto.setViewCount(post.getViewCount());
        dto.setCreatedAt(post.getCreatedAt().toString());
        dto.setCanEdit(owner);
        dto.setCanDelete(owner);

        dto.setImages(post.images.stream().map(PostImage::toDto).toList());

        return dto;
    }

    public static PostDTO toDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setAuthor(post.getAuthor().getNickName());
        dto.setBoard(post.getBoard().getName());
        dto.setBody(post.getBody());
        dto.setViewCount(post.getViewCount());
        dto.setCreatedAt(post.getCreatedAt().toString());
        dto.setCanEdit(false);
        dto.setCanDelete(false);
        dto.setImages(post.images.stream().map(PostImage::toDto).toList());

        return dto;
    }

    public boolean isAuthor(Long userId) {
        return author.getId().equals(userId);
    }

    public void addImage(PostImage image) {
        images.add(image);
        image.assignPost(this);
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}
