-- Conversation-scoped word effects for chat.
--
-- Effects live on the conversation rather than on a device, so the sender and every recipient of a
-- message resolve the same effect from the same rows, without the message, or the encryption
-- pipeline, being touched.
--
-- House style follows the other chat migrations: uuid keys, text user ids (public.chat_participants
-- stores user ids as text), get_current_user_uid() for the caller, explicit grants to authenticated,
-- and drop-policy-if-exists before each policy so the file can be re-run.

create table if not exists public.chat_word_effects (
  id uuid primary key default gen_random_uuid(),
  chat_id uuid not null references public.chats(id) on delete cascade,
  trigger text not null,
  emoji text not null,
  animation text not null default 'GLOW',
  is_enabled boolean not null default true,
  created_by text not null references public.users(uid) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chat_word_effects_trigger_length check (char_length(trigger) between 1 and 32),
  constraint chat_word_effects_emoji_length check (char_length(emoji) between 1 and 8),
  constraint chat_word_effects_animation_valid check (animation in ('GLOW', 'CONFETTI', 'SPARKLE', 'PULSE'))
);

-- One rule per trigger per conversation. The client checks for duplicates first so the user gets a
-- readable error; this index is what actually guarantees it when two devices write at once.
create unique index if not exists chat_word_effects_chat_trigger_key
  on public.chat_word_effects (chat_id, lower(trigger));

create index if not exists chat_word_effects_chat_id_idx
  on public.chat_word_effects (chat_id);

create index if not exists chat_word_effects_created_by_idx
  on public.chat_word_effects (created_by);

alter table public.chat_word_effects enable row level security;

grant select, insert, update, delete on public.chat_word_effects to authenticated;

-- Every participant of the conversation reads the effects, which is what keeps sender and recipient
-- views consistent.
drop policy if exists "Chat participants can read word effects" on public.chat_word_effects;
create policy "Chat participants can read word effects"
  on public.chat_word_effects
  for select to authenticated
  using (
    exists (
      select 1
      from public.chat_participants cp
      where cp.chat_id = chat_word_effects.chat_id
        and cp.user_id = get_current_user_uid()
    )
  );

drop policy if exists "Chat participants can add word effects" on public.chat_word_effects;
create policy "Chat participants can add word effects"
  on public.chat_word_effects
  for insert to authenticated
  with check (
    created_by = get_current_user_uid()
    and exists (
      select 1
      from public.chat_participants cp
      where cp.chat_id = chat_word_effects.chat_id
        and cp.user_id = get_current_user_uid()
    )
  );

drop policy if exists "Chat participants can edit word effects" on public.chat_word_effects;
create policy "Chat participants can edit word effects"
  on public.chat_word_effects
  for update to authenticated
  using (
    exists (
      select 1
      from public.chat_participants cp
      where cp.chat_id = chat_word_effects.chat_id
        and cp.user_id = get_current_user_uid()
    )
  )
  with check (
    exists (
      select 1
      from public.chat_participants cp
      where cp.chat_id = chat_word_effects.chat_id
        and cp.user_id = get_current_user_uid()
    )
  );

drop policy if exists "Chat participants can remove word effects" on public.chat_word_effects;
create policy "Chat participants can remove word effects"
  on public.chat_word_effects
  for delete to authenticated
  using (
    exists (
      select 1
      from public.chat_participants cp
      where cp.chat_id = chat_word_effects.chat_id
        and cp.user_id = get_current_user_uid()
    )
  );

-- updated_at is the database's job, so a client cannot leave a stale timestamp behind.
create or replace function public.set_chat_word_effects_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists chat_word_effects_set_updated_at on public.chat_word_effects;
create trigger chat_word_effects_set_updated_at
  before update on public.chat_word_effects
  for each row
  execute function public.set_chat_word_effects_updated_at();

comment on table public.chat_word_effects is
  'Word-triggered visual effects for a conversation. Read by every participant so sender and recipient render the same effect; effects never modify message content.';
