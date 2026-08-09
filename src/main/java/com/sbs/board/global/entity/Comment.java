package com.sbs.board.global.entity;

import com.sbs.board.comment.dto.CommentResponse;
import com.sbs.board.reaction.dto.ReactionResponse;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sbs.board.comment.dto.CommentResponse.DELETED_CONTENT;


@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글이 달릴 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 인증된 사용자만 댓글을 달 수 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    // 대댓글, 부모 댓글, 최상위 댓글이면 null --> 게시글(post)의 댓글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("createdAt asc")
    @BatchSize(size = 100)
    private List<Comment> children = new ArrayList<>();
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false)
    private boolean deleted;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 양방향 편의 메서드, 댓글의 parent와 children을 동시에 처리
    public void addReply(Comment reply) {
        children.add( reply );
        reply.parent = this;
    }

    public void update(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.deleted = true;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isReply() {
        return parent != null;
    }

    public boolean isAuthor(Long userId) {
        return user.getId().equals(userId);
    }

    // 반응 정보 없이 변환한다(좋아요/싫어요 0). 방금 생성한 댓글처럼 반응이 있을 수 없는 경우에 사용
    public static CommentResponse toResponse(Comment comment) {
        return toResponse(comment, Map.of());
    }

    // reactionSummaries: commentId -> 반응 집계. 목록 조회에서 배치로 모아온 결과를 넘긴다.
    // 목록에 없는 댓글은 0/null로 처리한다.
    public static CommentResponse toResponse(Comment comment, Map<Long, ReactionResponse> reactionSummaries) {
        List<CommentResponse> children = comment.isRoot() && comment.getChildren()!=null
                ? comment.getChildren().stream().map(child -> toResponse(child, reactionSummaries)).toList()
                : List.of();    // 빈 리스트

        ReactionResponse reaction = reactionSummaries.get(comment.getId());

        return new CommentResponse(
            comment.getId(),
            comment.getUser().getNickName(),
            comment.isDeleted() ? DELETED_CONTENT : comment.getContent(),
            comment.isDeleted(),
            comment.getCreatedAt(),
            reaction != null ? reaction.getLikeCount() : 0L,
            reaction != null ? reaction.getDislikeCount() : 0L,
            reaction != null ? reaction.getMyReaction() : null,
            children
        );
    }
}

// id           Long
// post_id      Long (FK)
// user_id      Long (FK)
// parent_id    Long (FK), NULL==최상위, 값 있음==대댓글
// content      String(Text)
// deleted      boolean(Soft delete)
// created_at   LocalDateTime

//


