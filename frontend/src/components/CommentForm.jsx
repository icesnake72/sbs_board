import { useState } from 'react';
import { Spinner } from './ui';

export default function CommentForm({
  onSubmit,
  onCancel,
  initialValue = '',
  placeholder = '댓글을 입력하세요',
  submitLabel = '등록',
  autoFocus = false,
}) {
  const [content, setContent] = useState(initialValue);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const handle = async (e) => {
    e.preventDefault();
    const value = content.trim();

    if (!value) return setError('내용을 입력하세요');
    if (value.length > 1000) return setError('댓글은 1000자 이하로 입력하세요');

    setError('');
    setBusy(true);
    try {
      await onSubmit(value);
      setContent('');
    } catch (err) {
      setError(err.userMessage ?? '등록에 실패했습니다');
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={handle} className="space-y-2">
      <textarea
        className={`input min-h-[80px] resize-y ${error ? 'input-error' : ''}`}
        placeholder={placeholder}
        value={content}
        maxLength={1000}
        autoFocus={autoFocus}
        disabled={busy}
        onChange={(e) => setContent(e.target.value)}
      />
      <div className="flex items-center justify-between">
        <span className="text-xs text-slate-400">{content.length}/1000</span>
        <div className="flex gap-2">
          {onCancel && (
            <button type="button" className="btn-secondary" onClick={onCancel} disabled={busy}>
              취소
            </button>
          )}
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy && <Spinner />}
            {submitLabel}
          </button>
        </div>
      </div>
      {error && <p className="field-error">{error}</p>}
    </form>
  );
}
