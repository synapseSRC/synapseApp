-- App inserts users with only id+username (UserProfileInsert maps auth UUID to "id" column)
-- Auto-populate uid from id if not provided

ALTER TABLE public.users ALTER COLUMN uid DROP NOT NULL;

CREATE OR REPLACE FUNCTION public.set_user_uid()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.uid IS NULL OR NEW.uid = '' THEN
    NEW.uid := NEW.id::TEXT;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS set_uid_on_insert ON public.users;
CREATE TRIGGER set_uid_on_insert
  BEFORE INSERT ON public.users
  FOR EACH ROW EXECUTE FUNCTION public.set_user_uid();
