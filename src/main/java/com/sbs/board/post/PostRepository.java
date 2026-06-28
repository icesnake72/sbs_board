package com.sbs.board.post;

import com.sbs.board.global.entity.Board;
import com.sbs.board.global.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByBoard(Board board);
    List<Post> findByBoardId(Long boardId);

    // 특정 게시글의 작성자 id를 가져옴
    @Query("select p.author.id from Post p where p.id = :id")
    Optional<Long> findAuthorIdById(Long id);
}
