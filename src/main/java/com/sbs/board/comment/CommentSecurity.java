package com.sbs.board.comment;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("commentSecurity")
@RequiredArgsConstructor
public class CommentSecurity {
    private final CommentRepository commentRepository;

    public boolean isAuthor(Long commentId, CustomUserDetails userDetails) {
        Long authorId = commentRepository.findAuthorIdById(commentId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POST_NOT_FOUND));

        // 댓글로부터 작성자 id를 찾고 인증된 사용자(userDetails)의 id와 비교
        return authorId.equals(userDetails.getId());
    }
}
