package com.sbs.board.comment;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.comment.dto.CommentCreateRequest;
import com.sbs.board.comment.dto.CommentResponse;
import com.sbs.board.global.entity.Comment;
import com.sbs.board.global.entity.Post;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.notification.CommentCreateEvent;
import com.sbs.board.post.PostRepository;
import com.sbs.board.reaction.ReactionService;
import com.sbs.board.reaction.dto.ReactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ReactionService reactionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommentResponse create(Long postId, Long userId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new BusinessException(ErrorCode.POST_NOT_FOUND));

        User author = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .user(author)
                .build();

        if ( request.getParentId() != null ) {
            // 부모 댓글인지 검증 작업 후 대입
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(()-> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

            if ( !parent.getPost().getId().equals(postId) ) {
                throw new BusinessException(ErrorCode.COMMENT_POST_MISMATCH);
            }

            if ( parent.isDeleted() ) {
                throw new BusinessException(ErrorCode.CANNOT_REPLY_TO_DELETED);
            }

            if ( parent.isReply() ) {
                throw new BusinessException(ErrorCode.CANNOT_REPLY_TO_REPLY);
            }

            parent.addReply(comment);
        }

        Comment savedComment = commentRepository.save(comment);

        // 이벤트를 발생시킴
        eventPublisher.publishEvent(new CommentCreateEvent(
                savedComment.getId(),
                postId,
                request.getParentId(),
                userId
        ));

        return Comment.toResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long postId, Long loginUserId, Pageable pageable) {
        if ( !postRepository.existsById(postId) ) {
            throw new NotFoundException(ErrorCode.POST_NOT_FOUND);
        }

        Page<Comment> rootComments = commentRepository.findByPostIdAndParentIsNull(postId, pageable);

        // 응답 트리에 들어갈 댓글(최상위 + 대댓글)의 id를 모두 모은다
        List<Long> commentIds = collectCommentIds(rootComments.getContent());

        // 댓글마다 조회하면 N+1이 되므로 반응 정보는 여기서 한 번에 가져온다
        Map<Long, ReactionResponse> reactionSummaries =
                reactionService.summarizeCommentReactions(commentIds, loginUserId);

        return rootComments.map(comment -> Comment.toResponse(comment, reactionSummaries));
    }

    // 최상위 댓글과 그 대댓글의 id를 함께 수집한다.
    // getChildren()은 지연 로딩이지만 @BatchSize(100)으로 묶여서 조회되고,
    // 어차피 응답을 만들 때 순회하므로 여기서 추가로 발생하는 쿼리는 없다.
    private List<Long> collectCommentIds(List<Comment> rootComments) {
        List<Long> commentIds = new ArrayList<>();

        for (Comment root : rootComments) {
            commentIds.add(root.getId());

            if (root.getChildren() != null) {
                for (Comment child : root.getChildren()) {
                    commentIds.add(child.getId());
                }
            }
        }

        return commentIds;
    }

    @Transactional
    public CommentResponse update(Long id, Long loginUserId, @Valid CommentCreateRequest request) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(()-> new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));

        // 삭제된(soft delete) 댓글은 수정할 수 없다
        if ( comment.isDeleted() ) {
            throw new BusinessException(ErrorCode.CANNOT_EDIT_DELETED);
        }

        comment.update(request.getContent());
        Comment savedComment = commentRepository.save(comment);

        // 수정된 댓글에도 기존 반응이 남아있으므로 0으로 내려가지 않도록 함께 조회한다
        Map<Long, ReactionResponse> reactionSummaries =
                reactionService.summarizeCommentReactions(List.of(savedComment.getId()), loginUserId);

        return Comment.toResponse(savedComment, reactionSummaries);
    }


    @Transactional
    public void delete(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(()-> new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));

        comment.softDelete();
        commentRepository.save(comment);
    }
}
