package com.sbs.board.comment;

import com.sbs.board.global.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // select * from comments c
    // join user u on c.user_id = u.id
    // where post_id=:postId and parent_id is null
    @EntityGraph(attributePaths = {"user"})
    Page<Comment> findByPostIdAndParentIsNull(Long postId, Pageable pageable);

    @Query("SELECT c.user.id FROM Comment c WHERE c.id = :id")
    Optional<Long> findAuthorIdById(@Param("id") Long commentId);
}
















