create table if not exists public.message_poll_votes (
  id uuid primary key default gen_random_uuid(),
  message_id uuid not null references public.messages(id) on delete cascade,
  chat_id uuid not null references public.chats(id) on delete cascade,
  user_id text not null references public.users(uid) on delete cascade,
  option_index integer not null check (option_index >= 0 and option_index <= 3),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (message_id, user_id)
);

create index if not exists message_poll_votes_chat_id_idx
  on public.message_poll_votes (chat_id);

create index if not exists message_poll_votes_message_id_idx
  on public.message_poll_votes (message_id);

alter table public.message_poll_votes enable row level security;

grant select, insert, update, delete on public.message_poll_votes to authenticated;

drop policy if exists "Chat participants can read message poll votes" on public.message_poll_votes;
create policy "Chat participants can read message poll votes"
  on public.message_poll_votes
  for select to authenticated
  using (
    exists (
      select 1
      from public.chat_participants cp
      where cp.chat_id = message_poll_votes.chat_id
        and cp.user_id = get_current_user_uid()
    )
  );

drop policy if exists "Participants can cast one poll vote" on public.message_poll_votes;
create policy "Participants can cast one poll vote"
  on public.message_poll_votes
  for insert to authenticated
  with check (
    user_id = get_current_user_uid()
    and exists (
      select 1
      from public.messages m
      join public.chat_participants cp on cp.chat_id = m.chat_id
      where m.id = message_poll_votes.message_id
        and m.chat_id = message_poll_votes.chat_id
        and lower(m.message_type) = 'poll'
        and cp.user_id = get_current_user_uid()
    )
  );

drop policy if exists "Users can update their own message poll vote" on public.message_poll_votes;
create policy "Users can update their own message poll vote"
  on public.message_poll_votes
  for update to authenticated
  using (
    user_id = get_current_user_uid()
    and exists (
      select 1
      from public.chat_participants cp
      where cp.chat_id = message_poll_votes.chat_id
        and cp.user_id = get_current_user_uid()
    )
  )
  with check (
    user_id = get_current_user_uid()
    and exists (
      select 1
      from public.messages m
      join public.chat_participants cp on cp.chat_id = m.chat_id
      where m.id = message_poll_votes.message_id
        and m.chat_id = message_poll_votes.chat_id
        and lower(m.message_type) = 'poll'
        and cp.user_id = get_current_user_uid()
    )
  );

drop policy if exists "Users can remove their own message poll vote" on public.message_poll_votes;
create policy "Users can remove their own message poll vote"
  on public.message_poll_votes
  for delete to authenticated
  using (
    user_id = get_current_user_uid()
    and exists (
      select 1
      from public.chat_participants cp
      where cp.chat_id = message_poll_votes.chat_id
        and cp.user_id = get_current_user_uid()
    )
  );

do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'message_poll_votes'
  ) then
    alter publication supabase_realtime add table public.message_poll_votes;
  end if;
end $$;
