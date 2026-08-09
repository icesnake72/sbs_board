import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import * as boardApi from '@/api/board';
import * as postApi from '@/api/post';
import * as commentApi from '@/api/comment';
import * as notifyApi from '@/api/notification';
import { useAuthStore } from '@/stores/auth';
import { PAGE_SIZE } from '@/lib/constants';

export const qk = {
  boards: ['boards'],
  posts: (boardId, page, sort) => ['posts', boardId, page, sort],
  post: (id) => ['post', id],
  comments: (postId, page) => ['comments', postId, page],
  unread: ['notify', 'unread'],
  notifications: (page) => ['notify', 'list', page],
};

// ── 게시판 ──────────────────────────────────────────────
// 자주 바뀌지 않으므로 길게 캐싱한다.
export const useBoards = () =>
  useQuery({ queryKey: qk.boards, queryFn: boardApi.getBoards, staleTime: 5 * 60_000 });

export const useBoardMutations = () => {
  const qc = useQueryClient();
  const invalidate = () => qc.invalidateQueries({ queryKey: qk.boards });

  return {
    create: useMutation({ mutationFn: boardApi.createBoard, onSuccess: invalidate }),
    update: useMutation({
      mutationFn: ({ id, body }) => boardApi.updateBoard(id, body),
      onSuccess: invalidate,
    }),
    remove: useMutation({ mutationFn: boardApi.deleteBoard, onSuccess: invalidate }),
  };
};

// ── 게시글 ──────────────────────────────────────────────
export const usePosts = (boardId, page = 0, sort = 'createdAt,DESC', enabled = true) =>
  useQuery({
    queryKey: qk.posts(boardId, page, sort),
    queryFn: () => postApi.getPosts(boardId, page, PAGE_SIZE, sort),
    placeholderData: keepPreviousData, // 페이지 전환 시 깜빡임 방지
    enabled: enabled && boardId != null,
  });

export const usePost = (id) =>
  useQuery({ queryKey: qk.post(id), queryFn: () => postApi.getPost(id), enabled: id != null, retry: false });

export const useCreatePost = (boardId) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ post, images }) => postApi.createPost(boardId, post, images),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['posts', boardId] }),
  });
};

export const useUpdatePost = (id) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (post) => postApi.updatePost(id, post),
    onSuccess: (data) => {
      qc.setQueryData(qk.post(id), data);
      qc.invalidateQueries({ queryKey: ['posts'] });
    },
  });
};

export const useDeletePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: postApi.deletePost,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['posts'] }),
  });
};

// ── 댓글 ────────────────────────────────────────────────
export const useComments = (postId, page = 0) =>
  useQuery({
    queryKey: qk.comments(postId, page),
    queryFn: () => commentApi.getComments(postId, page),
    placeholderData: keepPreviousData,
    enabled: postId != null,
  });

/** 작성 응답만으로 트리를 재구성하기 어려우므로 목록을 재조회한다. */
export const useCommentMutations = (postId) => {
  const qc = useQueryClient();
  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['comments', postId] });
    qc.invalidateQueries({ queryKey: qk.unread });
  };

  return {
    create: useMutation({
      mutationFn: ({ content, parentId }) => commentApi.createComment(postId, content, parentId),
      onSuccess: invalidate,
    }),
    update: useMutation({
      mutationFn: ({ id, content }) => commentApi.updateComment(id, content),
      onSuccess: invalidate,
    }),
    remove: useMutation({ mutationFn: commentApi.deleteComment, onSuccess: invalidate }),
  };
};

// ── 알림 ────────────────────────────────────────────────
/** 서버 푸시가 없어 30초 폴링한다. COUNT 쿼리 1회라 가볍다. */
export const useUnreadCount = () => {
  const loggedIn = useAuthStore((s) => !!s.accessToken);
  return useQuery({
    queryKey: qk.unread,
    queryFn: notifyApi.getUnreadCount,
    refetchInterval: 30_000,
    enabled: loggedIn,
  });
};

/** 드롭다운을 열었을 때만 조회한다. */
export const useNotifications = (page = 0, enabled = false) =>
  useQuery({
    queryKey: qk.notifications(page),
    queryFn: () => notifyApi.getNotifications(page),
    enabled,
  });

export const useReadNotification = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: notifyApi.readNotification,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.unread });
      qc.invalidateQueries({ queryKey: ['notify', 'list'] });
    },
  });
};
