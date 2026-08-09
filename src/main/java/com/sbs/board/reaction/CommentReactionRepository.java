package com.sbs.board.reaction;

import com.sbs.board.global.entity.Comment;
import com.sbs.board.reaction.dto.ReactionCount;
import com.sbs.board.reaction.dto.UserReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);
    long countByCommentIdAndType(Long commentId, ReactionType type);

    // 댓글 목록용 배치 집계
    // 댓글마다 countByCommentIdAndType()을 호출하면 (댓글 수 x 2)만큼 쿼리가 나가므로(N+1),
    // GROUP BY로 한 번에 집계한다.
    @Query("SELECT new com.sbs.board.reaction.dto.ReactionCount(cr.comment.id, cr.type, COUNT(cr)) " +
            "FROM CommentReaction cr " +
            "WHERE cr.comment.id IN :commentIds " +
            "GROUP BY cr.comment.id, cr.type")
    List<ReactionCount> countGroupByCommentIdAndType(@Param("commentIds") Collection<Long> commentIds);

    // 댓글 목록용 배치 집계 - 로그인한 사용자 본인의 반응만 한 번에 조회
    @Query("SELECT new com.sbs.board.reaction.dto.UserReaction(cr.comment.id, cr.type) " +
            "FROM CommentReaction cr " +
            "WHERE cr.comment.id IN :commentIds AND cr.user.id = :userId")
    List<UserReaction> findUserReactionsByCommentIds(@Param("commentIds") Collection<Long> commentIds,
                                                     @Param("userId") Long userId);
}
