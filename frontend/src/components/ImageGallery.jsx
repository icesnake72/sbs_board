import { useEffect, useState } from 'react';
import { imageUrl } from '@/lib/api';

/**
 * 서버가 sortOrder 오름차순으로 정렬해 보내므로 그대로 렌더링한다.
 * 썸네일 API 가 없어 CSS object-fit 으로 처리한다.
 */
export default function ImageGallery({ images = [] }) {
  const [lightbox, setLightbox] = useState(null);

  useEffect(() => {
    if (lightbox === null) return;
    const onKey = (e) => {
      if (e.key === 'Escape') setLightbox(null);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [lightbox]);

  if (!images.length) return null;

  return (
    <>
      <ul className="grid grid-cols-2 gap-2 sm:grid-cols-3">
        {images.map((img, i) => (
          <li key={img.id}>
            <button
              type="button"
              className="block w-full overflow-hidden rounded-lg border border-slate-200"
              onClick={() => setLightbox(i)}
            >
              <img
                src={imageUrl(img.url)}
                alt={img.originalName}
                loading="lazy"
                className="h-40 w-full object-cover transition hover:scale-105"
              />
            </button>
          </li>
        ))}
      </ul>

      {lightbox !== null && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={images[lightbox].originalName}
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
          onClick={() => setLightbox(null)}
        >
          <img
            src={imageUrl(images[lightbox].url)}
            alt={images[lightbox].originalName}
            className="max-h-full max-w-full object-contain"
          />
          <button
            type="button"
            className="absolute right-4 top-4 rounded-full bg-white/90 px-3 py-1.5 text-sm font-medium"
            onClick={() => setLightbox(null)}
          >
            닫기
          </button>
        </div>
      )}
    </>
  );
}
