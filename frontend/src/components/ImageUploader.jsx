import { useEffect, useRef, useState } from 'react';
import { ALLOWED_IMAGE_TYPES, MAX_IMAGE_COUNT, MAX_IMAGE_SIZE } from '@/lib/constants';

/**
 * 드래그앤드롭 + 미리보기 + 개별 제거.
 * 배열 순서가 곧 전송 순서이며 서버의 sortOrder 가 된다.
 * 서버 400 을 피하려고 크기·타입을 클라이언트에서 미리 검증한다.
 */
export default function ImageUploader({ files, onChange, disabled }) {
  const inputRef = useRef(null);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState('');
  const [previews, setPreviews] = useState([]);

  // objectURL 은 반드시 해제한다 (메모리 누수 방지).
  useEffect(() => {
    const urls = files.map((f) => URL.createObjectURL(f));
    setPreviews(urls);
    return () => urls.forEach((u) => URL.revokeObjectURL(u));
  }, [files]);

  const accept = (incoming) => {
    setError('');
    const next = [...files];

    for (const f of incoming) {
      if (next.length >= MAX_IMAGE_COUNT) {
        setError(`이미지는 최대 ${MAX_IMAGE_COUNT}장까지 첨부할 수 있습니다`);
        break;
      }
      if (!ALLOWED_IMAGE_TYPES.includes(f.type)) {
        setError('png, jpg, jpeg, gif, webp 형식만 첨부할 수 있습니다');
        continue;
      }
      if (f.size > MAX_IMAGE_SIZE) {
        setError(`"${f.name}" 은 2MB를 초과합니다`);
        continue;
      }
      next.push(f);
    }

    onChange(next);
  };

  const remove = (i) => {
    setError('');
    onChange(files.filter((_, idx) => idx !== i));
  };

  return (
    <div>
      <div
        onDragOver={(e) => {
          e.preventDefault();
          if (!disabled) setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragging(false);
          if (!disabled) accept(Array.from(e.dataTransfer.files));
        }}
        className={`rounded-lg border-2 border-dashed p-6 text-center transition ${
          dragging ? 'border-brand-500 bg-brand-50' : 'border-slate-300 bg-slate-50'
        }`}
      >
        <p className="text-sm text-slate-600">이미지를 끌어다 놓거나</p>
        <button
          type="button"
          className="btn-secondary mt-2"
          disabled={disabled || files.length >= MAX_IMAGE_COUNT}
          onClick={() => inputRef.current?.click()}
        >
          파일 선택
        </button>
        <p className="mt-2 text-xs text-slate-500">
          최대 {MAX_IMAGE_COUNT}장 · 각 2MB 이하 · png/jpg/jpeg/gif/webp ({files.length}/
          {MAX_IMAGE_COUNT})
        </p>

        <input
          ref={inputRef}
          type="file"
          multiple
          accept={ALLOWED_IMAGE_TYPES.join(',')}
          className="hidden"
          onChange={(e) => {
            accept(Array.from(e.target.files ?? []));
            e.target.value = ''; // 같은 파일 재선택 허용
          }}
        />
      </div>

      {error && <p className="field-error">{error}</p>}

      {files.length > 0 && (
        <ul className="mt-3 grid grid-cols-3 gap-2 sm:grid-cols-5">
          {files.map((f, i) => (
            <li key={`${f.name}-${i}`} className="relative">
              <img
                src={previews[i]}
                alt={f.name}
                className="h-24 w-full rounded-lg border border-slate-200 object-cover"
              />
              <span className="absolute left-1 top-1 rounded bg-black/60 px-1.5 text-xs text-white">
                {i + 1}
              </span>
              <button
                type="button"
                aria-label={`${f.name} 제거`}
                className="absolute right-1 top-1 rounded-full bg-black/60 px-1.5 text-xs text-white hover:bg-black/80"
                onClick={() => remove(i)}
                disabled={disabled}
              >
                ✕
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
