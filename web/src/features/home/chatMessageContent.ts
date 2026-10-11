const isRecord = (value: unknown): value is Record<string, unknown> => Boolean(value && typeof value === 'object' && !Array.isArray(value));

/** Recognizes the Android chat client's encrypted-message envelope without exposing ciphertext. */
export const isEncryptedMessageContent = (content: string | null | undefined) => {
  if (!content) return false;
  try {
    const parsed: unknown = JSON.parse(content);
    if (!isRecord(parsed)) return false;
    const isCipher = (value: unknown) => isRecord(value) && Number.isInteger(value.type) && typeof value.body === 'string';
    return isCipher(parsed) || Object.values(parsed).some(isCipher);
  } catch {
    return false;
  }
};

/** Extracts the Android plaintext wrapper when present; otherwise returns ordinary text unchanged. */
export const getMessageDisplayText = (content: string | null) => {
  if (!content) return '';
  try {
    const parsed: unknown = JSON.parse(content);
    if (isRecord(parsed) && typeof parsed.content === 'string' && !isEncryptedMessageContent(content)) return parsed.content;
  } catch {
    // Regular text is not JSON.
  }
  return content;
};
