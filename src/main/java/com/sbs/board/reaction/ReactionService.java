package com.sbs.board.reaction;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.comment.CommentRepository;
import com.sbs.board.global.entity.Comment;
import com.sbs.board.global.entity.Post;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.post.PostRepository;
import com.sbs.board.reaction.dto.ReactionCount;
import com.sbs.board.reaction.dto.ReactionResponse;
import com.sbs.board.reaction.dto.UserReaction;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final PostReactionRepository postReactionRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReactionResponse react(Long postId, Long userId, @NotNull ReactionType type) {
        // post가 있는지 검사하고 없으면 에러 발생
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException(ErrorCode.POST_NOT_FOUND);
        };

        // postId와 userId를 이용하여 reaction이 존재하는지 여부 확인
        // 존재하면 이미 사용자가 react를 한 post이므로 type만 수정함
        // 존재하지 않으면 반응하지 않은 post이므로 reaction record를 생성함
        postReactionRepository.findByPostIdAndUserId(postId, userId).ifPresentOrElse(
            (postReaction) -> {
                // type과 postReaction의 타입이 동일하면 취소(해당 reaction 삭제)
                if ( postReaction.getType() == type)
                    postReactionRepository.delete(postReaction);
                else
                    postReaction.changeType(type);
            },

            () -> {
                // findById 와 getReferenceById
                // findById 는 반환값이 Optional<T>로 null safety 객체를 반환
                // getReferenceById 는 엔티티 인스턴스를 즉시 반환 -> exception 발생될 수 있음
                Post post = postRepository.getReferenceById(postId);
                User user = userRepository.getReferenceById(userId);
                postReactionRepository.save(PostReaction.builder()
                        .post(post)
                        .user(user)
                        .type(type)
                        .build());
            }
        );

        // ReactionResponse 인스턴스를 생성하여 반환
        // postReactionRespository의 countByPostIdAndType()을 이용하여
        // LIKE, DISLIKE 개수를 확인할 수 있음
        // findByPostIdAndUserId()를 이용하여 해당 사용자의 PostReaction을 가져올 수 있음
        // 위 3개의 정보를 이용하여 ReactionResponse 인스턴스를 생성할 수 있음
        return buildPostReactionResponse(postId, userId);
    }

    public ReactionResponse buildPostReactionResponse(Long postId, Long userId) {
        // 해당 게시글(postId)의 좋아요(like)와 싫어요(disLike) 개수 정보를 구한다.
        long like = postReactionRepository.countByPostIdAndType(postId, ReactionType.LIKE);
        long disLike = postReactionRepository.countByPostIdAndType(postId, ReactionType.DISLIKE);

        // reaction 레코드를 가져와서 getType()으로 ReactionType을 가져오거나 없으면 null로 대입
        ReactionType myReaction = postReactionRepository.findByPostIdAndUserId(postId, userId)
                .map(PostReaction::getType)
                .orElse(null);

        // ReactionResponse인스턴스를 반환
        return new ReactionResponse(like, disLike, myReaction);
    }

    @Transactional
    public ReactionResponse reactToComment(Long commentId, Long userId, @NotNull ReactionType type) {
        // comment가 있는지 검사하고 없으면 에러 발생
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // commentId와 userId를 이용하여 reaction이 존재하는지 여부 확인
        // 존재하면 이미 사용자가 react를 한 comment이므로 type만 수정함
        // 존재하지 않으면 반응하지 않은 comment이므로 reaction record를 생성함
        commentReactionRepository.findByCommentIdAndUserId(commentId, userId).ifPresentOrElse(
            (commentReaction) -> {
                // type과 commentReaction의 타입이 동일하면 취소(해당 reaction 삭제)
                if ( commentReaction.getType() == type)
                    commentReactionRepository.delete(commentReaction);
                else
                    commentReaction.changeType(type);
            },

            () -> {
                // 연관 엔티티는 실제 조회 없이 프록시(getReferenceById)로만 참조하여 INSERT 한다
                Comment comment = commentRepository.getReferenceById(commentId);
                User user = userRepository.getReferenceById(userId);
                commentReactionRepository.save(CommentReaction.builder()
                        .comment(comment)
                        .user(user)
                        .type(type)
                        .build());
            }
        );

        return buildCommentReactionResponse(commentId, userId);
    }

    public ReactionResponse buildCommentReactionResponse(Long commentId, Long userId) {
        // 해당 댓글(commentId)의 좋아요(like)와 싫어요(disLike) 개수 정보를 구한다.
        long like = commentReactionRepository.countByCommentIdAndType(commentId, ReactionType.LIKE);
        long disLike = commentReactionRepository.countByCommentIdAndType(commentId, ReactionType.DISLIKE);

        // reaction 레코드를 가져와서 getType()으로 ReactionType을 가져오거나 없으면 null로 대입
        ReactionType myReaction = commentReactionRepository.findByCommentIdAndUserId(commentId, userId)
                .map(CommentReaction::getType)
                .orElse(null);

        // ReactionResponse인스턴스를 반환
        return new ReactionResponse(like, disLike, myReaction);
    }

    // 게시글 목록의 반응 정보를 한 번에 모아서 반환한다.
    // 게시글이 몇 개든 쿼리 2회(비로그인이면 1회)로 끝난다.
    @Transactional(readOnly = true)
    public Map<Long, ReactionResponse> summarizePostReactions(Collection<Long> postIds, Long userId) {
        // IN절에 빈 목록이 들어가면 안되므로 조회 없이 반환
        if (postIds == null || postIds.isEmpty()) {
            return new HashMap<>();
        }

        List<ReactionCount> counts = postReactionRepository.countGroupByPostIdAndType(postIds);
        List<UserReaction> userReactions = userId == null
                ? List.of()     // 비로그인이면 본인 반응을 조회할 필요가 없다
                : postReactionRepository.findUserReactionsByPostIds(postIds, userId);

        return mergeSummaries(postIds, counts, userReactions);
    }

    // 댓글 목록(대댓글 포함)의 반응 정보를 한 번에 모아서 반환한다.
    // buildCommentReactionResponse()를 댓글마다 호출하면 (댓글 수 x 3)만큼 쿼리가 나가므로,
    // 댓글이 몇 개든 쿼리 2회(비로그인이면 1회)로 끝나도록 배치 집계를 사용한다.
    @Transactional(readOnly = true)
    public Map<Long, ReactionResponse> summarizeCommentReactions(Collection<Long> commentIds, Long userId) {
        if (commentIds == null || commentIds.isEmpty()) {
            return new HashMap<>();
        }

        List<ReactionCount> counts = commentReactionRepository.countGroupByCommentIdAndType(commentIds);
        List<UserReaction> userReactions = userId == null
                ? List.of()
                : commentReactionRepository.findUserReactionsByCommentIds(commentIds, userId);

        return mergeSummaries(commentIds, counts, userReactions);
    }

    // 배치로 조회한 집계 결과를 targetId -> ReactionResponse 형태로 합친다.
    // 반환 map은 요청한 모든 targetId를 key로 갖는다(반응이 없으면 0으로 채워짐).
    private Map<Long, ReactionResponse> mergeSummaries(Collection<Long> targetIds,
                                                       List<ReactionCount> counts,
                                                       List<UserReaction> userReactions) {
        Map<Long, ReactionResponse> summaries = new HashMap<>();

        // 조회 결과가 없는 대상도 null 없이 쓸 수 있도록 0으로 초기화해둔다
        for (Long targetId : targetIds) {
            summaries.put(targetId, new ReactionResponse(0L, 0L, null));
        }

        for (ReactionCount row : counts) {
            ReactionResponse summary = summaries.get(row.targetId());
            if (summary == null) {
                continue;
            }

            if (row.type() == ReactionType.LIKE) {
                summary.setLikeCount(row.count());
            } else {
                summary.setDislikeCount(row.count());
            }
        }

        for (UserReaction row : userReactions) {
            ReactionResponse summary = summaries.get(row.targetId());
            if (summary != null) {
                summary.setMyReaction(row.type());
            }
        }

        return summaries;
    }
}


