export const SYRA_TYPING_MAX_AGE_MS = 120_000;
const ALLOWED_CLOCK_SKEW_MS = 30_000;

export interface SyraInvocationStatusRow {
  request_id: string;
  status: string;
  user_id: string;
  chat_id: string;
  created_at: string;
}

export const isRecentSyraProcessingTimestamp = (createdAt: string, now = Date.now()) => {
  const createdAtMs = Date.parse(createdAt);
  return Number.isFinite(createdAtMs) &&
    createdAtMs >= now - SYRA_TYPING_MAX_AGE_MS &&
    createdAtMs <= now + ALLOWED_CLOCK_SKEW_MS;
};

export const isFreshSyraInvocation = (
  row: Partial<SyraInvocationStatusRow>,
  userId: string,
  chatId: string,
  now = Date.now(),
) => Boolean(
  row.request_id &&
  row.status === 'processing' &&
  row.user_id === userId &&
  row.chat_id === chatId &&
  row.created_at &&
  isRecentSyraProcessingTimestamp(row.created_at, now)
);

export const collectActiveSyraRequests = (
  rows: Partial<SyraInvocationStatusRow>[],
  userId: string,
  chatId: string,
  now = Date.now(),
): Record<string, string> => Object.fromEntries(
  rows
    .filter(row => isFreshSyraInvocation(row, userId, chatId, now))
    .map(row => [row.request_id!, row.created_at!]),
);
