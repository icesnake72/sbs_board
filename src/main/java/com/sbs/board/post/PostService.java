package com.sbs.board.post;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.board.BoardRepository;
import com.sbs.board.global.entity.Board;
import com.sbs.board.global.entity.Post;
import com.sbs.board.global.entity.PostImage;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.ForbiddenException;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.global.exception.UnauthorizedException;
import com.sbs.board.post.dto.PostRequest;
import com.sbs.board.post.dto.PostDTO;
import com.sbs.board.reaction.ReactionService;
import com.sbs.board.reaction.dto.ReactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ReactionService reactionService;

    public User requiredLogin(Long loginUserId) {
        if (loginUserId == null) {
            throw new UnauthorizedException(ErrorCode.LOGIN_REQUIRED);
        }

        return userRepository.findById(loginUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public PostDTO create(Long boardId, Long loginUserId, PostRequest request, List<MultipartFile> images) {
        System.out.println("Board ID: " + boardId);
        System.out.println("User ID: " + loginUserId);

//        User user = requiredLogin(loginUserId);
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));

        User user = userRepository.findById(loginUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        List<String> storedNames = new ArrayList<>();
        try {
            Post post = new Post();
            post.setTitle(request.getTitle());
            post.setBody(request.getBody());
            post.setBoard(board);
            post.setAuthor(user);

            // required == false 이므로 null이면 처리하지 않는다.
            if ( images != null ) {
                int order = 0;
                for(MultipartFile file: images) {
                    String storedName = fileStorageService.store(file);
                    storedNames.add(storedName);
                    PostImage image = PostImage.builder()
                                    .storedName(storedName) // 실제 저장된 경로 + uuid파일명
                                    .originalName(file.getOriginalFilename())
                                    .contentType(file.getContentType())
                                    .size(file.getSize())
                                    .sortOrder(order++)
                                    .build();

                    post.addImage(image);
                }
            }

            Post savedPost = postRepository.save( post );
            return Post.toDTO( savedPost );
        } catch (RuntimeException ex) {
            // 삭제 처리를 해야됨
            for(String fileName : storedNames) {
                fileStorageService.delete(fileName);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<PostDTO> list(Long loginUserId) {
        List<Post> posts = postRepository.findAll();

        // 게시글마다 조회하면 N+1이 되므로 반응 정보는 여기서 한 번에 가져온다
        Map<Long, ReactionResponse> reactionSummaries = reactionService.summarizePostReactions(
                posts.stream().map(Post::getId).toList(), loginUserId);

        return posts.stream()
                .map(post -> applyReaction(Post.toDTO(post), post.getId(), reactionSummaries))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PostDTO> findByBoardId(Long boardId, Long loginUserId, Pageable pageable) {
//        Board board = boardRepository.findById(boardId)
//                .orElseThrow(()-> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));

        Page<Post> posts = postRepository.findByBoardId(boardId, pageable);

        Map<Long, ReactionResponse> reactionSummaries = reactionService.summarizePostReactions(
                posts.getContent().stream().map(Post::getId).toList(), loginUserId);

        return posts.map(post -> applyReaction(Post.toDTO(post), post.getId(), reactionSummaries));
    }

    @Transactional
    public PostDTO getPost(Long loginUserId, Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() ->new NotFoundException(ErrorCode.POST_NOT_FOUND));

        // viewCount 증가, 로그인한 사용자가 자신이 작성하지 않은 글을 볼때만 증가
        if (!post.isAuthor(loginUserId) && loginUserId!=null) {
            post.increaseViewCount();
        }

        Map<Long, ReactionResponse> reactionSummaries =
                reactionService.summarizePostReactions(List.of(id), loginUserId);

        return applyReaction(Post.toDTO(post, loginUserId), id, reactionSummaries);
    }

    // 배치로 모아온 반응 집계를 PostDTO에 채워넣는다. 반응이 없으면 0/null 그대로 둔다.
    private PostDTO applyReaction(PostDTO dto, Long postId, Map<Long, ReactionResponse> reactionSummaries) {
        ReactionResponse reaction = reactionSummaries.get(postId);

        if (reaction != null) {
            dto.setLike(reaction.getLikeCount());
            dto.setDisLike(reaction.getDislikeCount());
            dto.setMyReaction(reaction.getMyReaction());
        }

        return dto;
    }

    @Transactional
    public PostDTO update(Long postId, @Valid PostRequest request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POST_NOT_FOUND));

//        validateAuthor(post, loginUserId);
//
//        // 게시글 수정권한이 없다면 에러를 발생시킴
//        User user = post.getAuthor();
//        if (!Objects.equals(user.getId(), loginUserId)) {
//            throw new ForbiddenException(ErrorCode.POST_ACCESS_DENIED);
//        }

        post.setTitle(request.getTitle());
        post.setBody(request.getBody());

        Post savedPost = postRepository.save( post );

        return Post.toDTO( savedPost );
    }

    @Transactional
    public void delete(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POST_NOT_FOUND));

//        validateAuthor(post, loginUserId);
//
//        // 게시글 수정권한이 없다면 에러를 발생시킴
//        User user = post.getAuthor();
//        if (!Objects.equals(user.getId(), loginUserId)) {
//            throw new ForbiddenException(ErrorCode.POST_ACCESS_DENIED);
//        }

        postRepository.delete( post );
    }

    private void validateAuthor(Post post, Long userId) {
        if (!post.isAuthor(userId)) {
            throw new ForbiddenException(ErrorCode.POST_ACCESS_DENIED);
        }
    }

//    public PostDTO findById(Long id) {
//        return postRepository.findById(id).orElse(null);
//    }
}
