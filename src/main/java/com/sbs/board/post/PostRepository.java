package com.sbs.board.post;

import com.sbs.board.global.entity.Board;
import com.sbs.board.global.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByBoard(Board board);

    @EntityGraph(attributePaths = {"board", "author"})
    Page<Post> findByBoardId(Long boardId, Pageable pageable);

    @Query("select distinct p from Post p " +
            "join fetch p.board " +
            "join fetch p.author " +
            "left join fetch p.images " +
            "where p.id = :id")
    Optional<Post> findDetailById(@Param("id")Long id);

    // 특정 게시글의 작성자 id를 가져옴
    @Query("select p.author.id from Post p where p.id = :id")
    Optional<Long> findAuthorIdById(Long id);
}
