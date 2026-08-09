package com.sbs.board.reaction;

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
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);

    // 게시글의 Like/Dislike 개수
    long countByPostIdAndType(Long postId, ReactionType type);

    // 게시글 목록용 배치 집계
    // 게시글마다 countByPostIdAndType()을 호출하면 (게시글 수 x 2)만큼 쿼리가 나가므로(N+1),
    // GROUP BY로 한 번에 집계한다.
    @Query("SELECT new com.sbs.board.reaction.dto.ReactionCount(pr.post.id, pr.type, COUNT(pr)) " +
            "FROM PostReaction pr " +
            "WHERE pr.post.id IN :postIds " +
            "GROUP BY pr.post.id, pr.type")
    List<ReactionCount> countGroupByPostIdAndType(@Param("postIds") Collection<Long> postIds);

    // 게시글 목록용 배치 집계 - 로그인한 사용자 본인의 반응만 한 번에 조회
    @Query("SELECT new com.sbs.board.reaction.dto.UserReaction(pr.post.id, pr.type) " +
            "FROM PostReaction pr " +
            "WHERE pr.post.id IN :postIds AND pr.user.id = :userId")
    List<UserReaction> findUserReactionsByPostIds(@Param("postIds") Collection<Long> postIds,
                                                  @Param("userId") Long userId);
}
