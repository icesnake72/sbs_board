/**
 * 서버 timestamp 는 "2026-08-01T14:32:11.123" 형태(오프셋 없음)로 내려온다.
 * 오프셋이 없는 date-time 문자열은 로컬 시간으로 해석된다.
 */
function parse(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
}

const MIN = 60_000;
const HOUR = 60 * MIN;
const DAY = 24 * HOUR;

export function formatRelative(iso) {
  const d = parse(iso);
  if (!d) return '';
  const diff = Date.now() - d.getTime();

  if (diff < MIN) return '방금 전';
  if (diff < HOUR) return `${Math.floor(diff / MIN)}분 전`;
  if (diff < DAY) return `${Math.floor(diff / HOUR)}시간 전`;
  if (diff < 7 * DAY) return `${Math.floor(diff / DAY)}일 전`;
  return formatDate(iso);
}

export function formatDate(iso) {
  const d = parse(iso);
  if (!d) return '';
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())}`;
}

export function formatDateTime(iso) {
  const d = parse(iso);
  if (!d) return '';
  const p = (n) => String(n).padStart(2, '0');
  return `${formatDate(iso)} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

export const formatCount = (n) => new Intl.NumberFormat('ko-KR').format(n ?? 0);
