export interface PostActionSource {
  id: string;
  likes_count: number | null;
  comments_count: number | null;
  reshares_count: number | null;
}

export interface PostInteractionRow { post_id: string; user_id: string; }
export interface PostReplyRow { in_reply_to_post_id: string | null; }

export interface PostInteractionState {
  isLiked: boolean;
  likesCount: number;
  isReshared: boolean;
  resharesCount: number;
  isSaved: boolean;
  repliesCount: number;
}

const countByPost = <T extends { post_id: string }>(rows: T[]) => {
  const counts = new Map<string, number>();
  for (const row of rows) counts.set(row.post_id, (counts.get(row.post_id) || 0) + 1);
  return counts;
};

export const buildPostInteractionStates = (
  posts: PostActionSource[],
  likes: PostInteractionRow[],
  reshares: PostInteractionRow[],
  favorites: PostInteractionRow[],
  replies: PostReplyRow[],
  userId: string,
): Record<string, PostInteractionState> => {
  const likeCounts = countByPost(likes);
  const reshareCounts = countByPost(reshares);
  const favoriteIds = new Set(favorites.filter(row => row.user_id === userId).map(row => row.post_id));
  const likedIds = new Set(likes.filter(row => row.user_id === userId).map(row => row.post_id));
  const resharedIds = new Set(reshares.filter(row => row.user_id === userId).map(row => row.post_id));
  const replyCounts = new Map<string, number>();
  for (const reply of replies) {
    if (reply.in_reply_to_post_id) replyCounts.set(reply.in_reply_to_post_id, (replyCounts.get(reply.in_reply_to_post_id) || 0) + 1);
  }

  return Object.fromEntries(posts.map(post => [post.id, {
    isLiked: likedIds.has(post.id),
    likesCount: likeCounts.get(post.id) ?? 0,
    isReshared: resharedIds.has(post.id),
    resharesCount: reshareCounts.get(post.id) ?? 0,
    isSaved: favoriteIds.has(post.id),
    repliesCount: replyCounts.get(post.id) ?? 0,
  }]));
};

export const parseReplyUsernames = (value: unknown): string[] => {
  let parsed = value;
  if (typeof parsed === 'string') {
    const raw = parsed;
    try { parsed = JSON.parse(raw) as unknown; } catch { return raw.trim() ? [raw.trim().replace(/^@/, '')] : []; }
  }
  const entries = Array.isArray(parsed) ? parsed : parsed == null ? [] : [parsed];
  return [...new Set(entries.flatMap(entry => {
    if (typeof entry === 'string') return [entry.trim().replace(/^@/, '')].filter(Boolean);
    if (entry && typeof entry === 'object') {
      const row = entry as Record<string, unknown>;
      const username = row.username ?? row.user_name ?? row.handle;
      return typeof username === 'string' ? [username.trim().replace(/^@/, '')].filter(Boolean) : [];
    }
    return [];
  }))];
};
