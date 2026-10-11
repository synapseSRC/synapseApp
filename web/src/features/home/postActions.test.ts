import { describe, expect, it } from 'vitest';
import { buildPostInteractionStates, parseReplyUsernames } from './postActions';

describe('post interaction mapping', () => {
  it('derives per-post counts and the current user state from live rows', () => {
    const states = buildPostInteractionStates(
      [
        { id: 'p1', likes_count: 99, comments_count: 80, reshares_count: 70 },
        { id: 'p2', likes_count: null, comments_count: null, reshares_count: null },
      ],
      [{ post_id: 'p1', user_id: 'me' }, { post_id: 'p1', user_id: 'other' }],
      [{ post_id: 'p1', user_id: 'other' }],
      [{ post_id: 'p1', user_id: 'me' }],
      [{ in_reply_to_post_id: 'p1' }, { in_reply_to_post_id: 'p1' }, { in_reply_to_post_id: null }],
      'me',
    );

    expect(states.p1).toEqual({ isLiked: true, likesCount: 2, isReshared: false, resharesCount: 1, isSaved: true, repliesCount: 2 });
    expect(states.p2).toEqual({ isLiked: false, likesCount: 0, isReshared: false, resharesCount: 0, isSaved: false, repliesCount: 0 });
  });

  it('starts from zero interaction rows when the source tables are empty', () => {
    const states = buildPostInteractionStates(
      [{ id: 'p1', likes_count: 4, comments_count: 3, reshares_count: 2 }],
      [], [], [], [], 'me',
    );
    expect(states.p1).toEqual({ isLiked: false, likesCount: 0, isReshared: false, resharesCount: 0, isSaved: false, repliesCount: 0 });
  });
});

describe('reply username parsing', () => {
  it('normalizes @ handles from a json array', () => {
    expect(parseReplyUsernames('["@mira", "sam"]')).toEqual(['mira', 'sam']);
  });

  it('handles structured reply mentions and empty values', () => {
    expect(parseReplyUsernames([{ username: '@mira' }, { handle: 'sam' }, { name: 'ignored' }])).toEqual(['mira', 'sam']);
    expect(parseReplyUsernames(null)).toEqual([]);
  });
});
