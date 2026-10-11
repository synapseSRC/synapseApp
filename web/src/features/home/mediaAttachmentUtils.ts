export type MediaKind = 'image' | 'video' | 'audio' | 'file';

export interface MediaAttachment {
  url: string;
  type: MediaKind;
  name?: string;
  mimeType?: string;
  size?: number;
  duration?: number;
  width?: number;
  height?: number;
}

export const formatMediaDuration = (duration?: number) => {
  if (!Number.isFinite(duration) || !duration || duration < 0) return '';
  const totalSeconds = Math.floor(duration);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return hours > 0
    ? `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
    : `${minutes}:${String(seconds).padStart(2, '0')}`;
};

export const mediaResolutionLabel = (attachment: MediaAttachment) => {
  const longestSide = Math.max(attachment.width || 0, attachment.height || 0);
  if (longestSide >= 1920) return 'FHD';
  if (longestSide >= 1280) return 'HD';
  if (longestSide >= 720) return 'HD';
  return longestSide > 0 ? 'SD' : '';
};
