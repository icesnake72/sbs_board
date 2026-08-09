package com.sbs.board.comment;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.comment.dto.CommentCreateRequest;
import com.sbs.board.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/post/{postId}/new")
    public CommentResponse create(
            @PathVariable(name = "postId") Long postId, // 이럴때 name은 생략 가능
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CommentCreateRequest request
            ) {
        return commentService.create(postId, userDetails.getId(), request);
    }

    // 비로그인도 조회 가능하므로 userDetails가 null일 수 있다(myReaction이 null로 내려감)
    @GetMapping("/post/{postId}/list")
    public Page<CommentResponse> getComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long viewerId = userDetails == null ? null : userDetails.getId();

        return commentService.getComments(postId, viewerId, pageable);
    }

    // 작성자만 수정이 가능하도록 처리해야 한다!!!
    // authentication.principal: SecurityContextHolder에 인증된 사용자 정보가 있음
    @PreAuthorize("@commentSecurity.isAuthor(#id, authentication.principal)")
    @PutMapping("/{id}")
    public CommentResponse update(
        @PathVariable Long id,  // 여기서 이 id는 comment의 id이다
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CommentCreateRequest request) {
        return commentService.update(id, userDetails.getId(), request);
    }

    // 작성자만 삭제가 가능하도록 처리해야 한다!!!
    @PreAuthorize("@commentSecurity.isAuthor(#id, authentication.principal)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // 삭제 코드
        commentService.delete(id);

        return ResponseEntity.ok().build();
    }
}
