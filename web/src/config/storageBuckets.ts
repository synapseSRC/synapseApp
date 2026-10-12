const bucketName = (value: string | undefined, fallback: string) => value?.trim() || fallback;

export const STORAGE_BUCKETS = {
  AVATARS: bucketName(import.meta.env.VITE_AVATARS_BUCKET, 'avatars'),
  CHAT_MEDIA: bucketName(import.meta.env.VITE_CHAT_MEDIA_BUCKET, 'web-chat-media'),
  POSTS_MEDIA: bucketName(import.meta.env.VITE_POSTS_MEDIA_BUCKET, 'posts'),
} as const;
