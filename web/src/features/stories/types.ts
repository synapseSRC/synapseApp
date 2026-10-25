export type Story = {
  id: string;
  user_id: string;
  content: string | null;
  media_url: string | null;
  thumbnail_url: string | null;
  media_type: string | null;
  created_at: string | null;
  expires_at: string | null;
  is_active?: boolean | null;
};
