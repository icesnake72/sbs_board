import { useEffect } from 'react';
import { Route, Routes } from 'react-router-dom';
import { restoreSession } from '@/lib/api';
import { useAuthStore } from '@/stores/auth';
import Layout from '@/components/Layout';
import RequireAuth from '@/components/RequireAuth';
import HomePage from '@/pages/HomePage';
import BoardPostsPage from '@/pages/BoardPostsPage';
import PostDetailPage from '@/pages/PostDetailPage';
import PostNewPage from '@/pages/PostNewPage';
import PostEditPage from '@/pages/PostEditPage';
import LoginPage from '@/pages/LoginPage';
import SignupPage from '@/pages/SignupPage';
import OAuthCallbackPage from '@/pages/OAuthCallbackPage';
import AdminBoardsPage from '@/pages/AdminBoardsPage';
import NotFoundPage from '@/pages/NotFoundPage';

export default function App() {
  const setInitialized = useAuthStore((s) => s.setInitialized);

  // 부팅 시 1회: Refresh 쿠키로 Access Token 을 복원한다.
  // 실패하면 그냥 비로그인 상태다.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      await restoreSession();
      if (!cancelled) setInitialized(true);
    })();
    return () => {
      cancelled = true;
    };
  }, [setInitialized]);

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/boards/:boardId" element={<BoardPostsPage />} />
        <Route path="/posts/:postId" element={<PostDetailPage />} />
        <Route
          path="/boards/:boardId/posts/new"
          element={
            <RequireAuth>
              <PostNewPage />
            </RequireAuth>
          }
        />
        <Route
          path="/posts/:postId/edit"
          element={
            <RequireAuth>
              <PostEditPage />
            </RequireAuth>
          }
        />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
        {/* 🚧 role 판별 불가 — 메뉴에 노출하지 않고 직접 URL 로만 접근한다. */}
        <Route
          path="/admin/boards"
          element={
            <RequireAuth>
              <AdminBoardsPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
