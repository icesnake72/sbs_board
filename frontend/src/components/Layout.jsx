import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/auth';
import { useBoards } from '@/hooks/queries';
import { logout as logoutApi } from '@/api/auth';
import NotificationBell from './NotificationBell';
import { Toaster } from './ui';

export default function Layout() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { accessToken, user, clear } = useAuthStore();
  const { data: boards } = useBoards();
  const [menuOpen, setMenuOpen] = useState(false);

  const loggedIn = !!accessToken;

  const handleLogout = async () => {
    try {
      await logoutApi();
    } catch {
      /* 쿠키가 없어도 성공 처리한다 */
    }
    clear();
    qc.clear();
    navigate('/');
  };

  const boardLink = ({ isActive }) =>
    `whitespace-nowrap rounded-lg px-3 py-1.5 text-sm transition ${
      isActive ? 'bg-brand-50 font-medium text-brand-700' : 'text-slate-600 hover:bg-slate-100'
    }`;

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-5xl items-center gap-3 px-4">
          <Link to="/" className="text-lg font-bold text-brand-600">
            게시판
          </Link>

          <nav className="hidden flex-1 items-center gap-1 overflow-x-auto sm:flex">
            {boards?.map((b) => (
              <NavLink key={b.id} to={`/boards/${b.id}`} className={boardLink}>
                {b.name}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-2">
            {loggedIn && <NotificationBell />}
            {loggedIn ? (
              <>
                {user?.nickName && (
                  <span className="hidden text-sm text-slate-600 sm:inline">
                    {user.nickName}님
                  </span>
                )}
                <button type="button" className="btn-ghost" onClick={handleLogout}>
                  로그아웃
                </button>
              </>
            ) : (
              <Link to="/login" className="btn-primary">
                로그인
              </Link>
            )}
            <button
              type="button"
              aria-label="게시판 목록"
              className="btn-ghost px-2 sm:hidden"
              onClick={() => setMenuOpen((v) => !v)}
            >
              ☰
            </button>
          </div>
        </div>

        {menuOpen && (
          <nav className="flex flex-col gap-1 border-t border-slate-200 px-4 py-2 sm:hidden">
            {boards?.map((b) => (
              <NavLink
                key={b.id}
                to={`/boards/${b.id}`}
                className={boardLink}
                onClick={() => setMenuOpen(false)}
              >
                {b.name}
              </NavLink>
            ))}
          </nav>
        )}
      </header>

      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>

      <Toaster />
    </div>
  );
}
