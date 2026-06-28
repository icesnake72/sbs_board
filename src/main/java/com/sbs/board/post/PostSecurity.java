package com.sbs.board.post;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("postSecurity")
@RequiredArgsConstructor
public class PostSecurity {
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public boolean isAuthor(Long postId, CustomUserDetails user) {

        log.debug("PostSecurity.isAuthor user.email: {}", user.getUsername());


        Long authorId = postRepository.findAuthorIdById(postId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POST_NOT_FOUND));

        return authorId.equals(user.getId());
    }
}
