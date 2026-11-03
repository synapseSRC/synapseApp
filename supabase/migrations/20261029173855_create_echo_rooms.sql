-- Echo Rooms: ephemeral multi-user audio spaces.
--
-- Identity is the app uid (`users.uid`, text), not `auth.uid()` (uuid): every social table
-- (follows, blocks, chat_participants) keys on the text uid, and existing rows prove the two
-- values are not interchangeable. LiveKit's participant identity is this same text uid, so SDK
-- participants map 1:1 to rows here.

create table public.echo_rooms (
    id uuid primary key default gen_random_uuid(),
    host_id text not null references public.users(uid) on delete cascade,
    title text not null,
    topic text,
    visibility text not null default 'public',
    status text not null default 'live',
    max_speakers integer not null default 8,
    livekit_room text not null unique,
    created_at timestamptz not null default now(),
    ended_at timestamptz,
    constraint echo_rooms_title_len check (char_length(btrim(title)) between 1 and 80),
    constraint echo_rooms_topic_len check (topic is null or char_length(topic) <= 200),
    constraint echo_rooms_visibility check (visibility in ('public', 'unlisted')),
    constraint echo_rooms_status check (status in ('live', 'ended')),
    constraint echo_rooms_max_speakers check (max_speakers between 2 and 20)
);

-- One live room per host makes create idempotent (a retry cannot start a second room) and
-- bounds LiveKit concurrency held by one account.
create unique index echo_rooms_one_live_per_host
    on public.echo_rooms (host_id)
    where status = 'live';

create index echo_rooms_lobby_idx
    on public.echo_rooms (status, visibility, created_at desc);

create table public.echo_room_participants (
    id uuid primary key default gen_random_uuid(),
    room_id uuid not null references public.echo_rooms(id) on delete cascade,
    user_id text not null references public.users(uid) on delete cascade,
    role text not null default 'audience',
    hand_raised boolean not null default false,
    joined_at timestamptz not null default now(),
    last_seen_at timestamptz not null default now(),
    left_at timestamptz,
    constraint echo_participants_role check (role in ('host', 'speaker', 'audience')),
    constraint echo_participants_unique unique (room_id, user_id)
);

create index echo_participants_live_idx
    on public.echo_room_participants (room_id)
    where left_at is null;

create index echo_participants_user_idx
    on public.echo_room_participants (user_id)
    where left_at is null;

alter table public.echo_rooms enable row level security;
alter table public.echo_room_participants enable row level security;

-- SECURITY DEFINER avoids the participants SELECT policy querying its own table, which would
-- recurse (42P17) the moment a policy needed membership.
create or replace function public.synapse_is_echo_member(p_room uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.echo_room_participants p
        where p.room_id = p_room
          and p.user_id = public.get_current_user_uid()
          and p.left_at is null
    );
$$;

revoke all on function public.synapse_is_echo_member(uuid) from public, anon;
grant execute on function public.synapse_is_echo_member(uuid) to authenticated;

create policy echo_rooms_select_visible
    on public.echo_rooms
    for select
    to authenticated
    using (
        host_id = public.get_current_user_uid()
        or public.synapse_is_echo_member(id)
    );

create policy echo_participants_select_member
    on public.echo_room_participants
    for select
    to authenticated
    using (public.synapse_is_echo_member(room_id));

-- Clients never write room state directly; every mutation goes through the RPCs below.
revoke all on table public.echo_rooms from anon, authenticated;
revoke all on table public.echo_room_participants from anon, authenticated;
grant select on table public.echo_rooms to authenticated;
grant select on table public.echo_room_participants to authenticated;

create or replace function public.expire_echo_rooms()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    v_count integer := 0;
begin
    with ended as (
        update public.echo_rooms r
        set status = 'ended',
            ended_at = now()
        where r.status = 'live'
          and (
              r.created_at < now() - interval '6 hours'
              -- The host disappearing is the room ending, even if listeners are still connected.
              or not exists (
                  select 1
                  from public.echo_room_participants host
                  where host.room_id = r.id
                    and host.user_id = r.host_id
                    and host.left_at is null
                    and host.last_seen_at > now() - interval '2 minutes'
              )
              or not exists (
                  select 1
                  from public.echo_room_participants p
                  where p.room_id = r.id
                    and p.left_at is null
                    and p.last_seen_at > now() - interval '2 minutes'
              )
          )
        returning 1
    )
    select count(*) into v_count from ended;

    update public.echo_room_participants p
    set left_at = now(),
        hand_raised = false
    where p.left_at is null
      and exists (
          select 1 from public.echo_rooms r
          where r.id = p.room_id and r.status = 'ended'
      );

    return v_count;
end;
$$;

revoke all on function public.expire_echo_rooms() from public, anon;
grant execute on function public.expire_echo_rooms() to authenticated;

create or replace function public.create_echo_room(
    p_title text,
    p_topic text default null,
    p_visibility text default 'public',
    p_max_speakers integer default 8
)
returns public.echo_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_title text := btrim(coalesce(p_title, ''));
    v_topic text := nullif(btrim(coalesce(p_topic, '')), '');
    v_visibility text := coalesce(p_visibility, 'public');
    v_max integer := greatest(2, least(coalesce(p_max_speakers, 8), 20));
    v_existing public.echo_rooms;
    v_room public.echo_rooms;
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;
    if not exists (select 1 from public.users u where u.uid = v_uid) then
        raise exception 'SYNAPSE_ECHO_PROFILE_REQUIRED' using errcode = '42501';
    end if;
    if exists (select 1 from public.users u where u.uid = v_uid and coalesce(u.banned, false)) then
        raise exception 'SYNAPSE_ECHO_BANNED' using errcode = '42501';
    end if;
    if v_title = '' or char_length(v_title) > 80 then
        raise exception 'SYNAPSE_ECHO_INVALID_TITLE' using errcode = '22023';
    end if;
    if v_topic is not null and char_length(v_topic) > 200 then
        raise exception 'SYNAPSE_ECHO_INVALID_TOPIC' using errcode = '22023';
    end if;
    if v_visibility not in ('public', 'unlisted') then
        raise exception 'SYNAPSE_ECHO_INVALID_VISIBILITY' using errcode = '22023';
    end if;

    perform public.expire_echo_rooms();

    select * into v_existing
    from public.echo_rooms r
    where r.host_id = v_uid and r.status = 'live'
    limit 1;

    if found then
        -- Same host tapping create twice gets the room they already own.
        return v_existing;
    end if;

    insert into public.echo_rooms (host_id, title, topic, visibility, max_speakers, livekit_room)
    values (v_uid, v_title, v_topic, v_visibility, v_max, 'echo-' || gen_random_uuid()::text)
    returning * into v_room;

    insert into public.echo_room_participants (room_id, user_id, role, joined_at, last_seen_at)
    values (v_room.id, v_uid, 'host', now(), now())
    on conflict (room_id, user_id) do update
        set role = 'host',
            left_at = null,
            joined_at = now(),
            last_seen_at = now(),
            hand_raised = false;

    return v_room;
end;
$$;

revoke all on function public.create_echo_room(text, text, text, integer) from public, anon;
grant execute on function public.create_echo_room(text, text, text, integer) to authenticated;

create or replace function public.join_echo_room(p_room_id uuid)
returns table (role text)
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_room public.echo_rooms;
    v_role text;
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;
    if p_room_id is null then
        raise exception 'SYNAPSE_ECHO_NOT_FOUND' using errcode = 'P0002';
    end if;
    if not exists (select 1 from public.users u where u.uid = v_uid) then
        raise exception 'SYNAPSE_ECHO_PROFILE_REQUIRED' using errcode = '42501';
    end if;
    if exists (select 1 from public.users u where u.uid = v_uid and coalesce(u.banned, false)) then
        raise exception 'SYNAPSE_ECHO_BANNED' using errcode = '42501';
    end if;

    perform public.expire_echo_rooms();

    select * into v_room from public.echo_rooms r where r.id = p_room_id;
    if not found then
        raise exception 'SYNAPSE_ECHO_NOT_FOUND' using errcode = 'P0002';
    end if;
    if v_room.status <> 'live' then
        raise exception 'SYNAPSE_ECHO_ENDED' using errcode = '55000';
    end if;

    if exists (
        select 1
        from public.blocks b
        where (b.blocker_id = v_uid and b.blocked_id = v_room.host_id)
           or (b.blocker_id = v_room.host_id and b.blocked_id = v_uid)
    ) then
        raise exception 'SYNAPSE_ECHO_BLOCKED' using errcode = '42501';
    end if;

    -- One audio room at a time per account: this process owns exactly one LiveKit room, and a
    -- second membership would leave a live row nobody is actually in.
    update public.echo_rooms
    set status = 'ended',
        ended_at = now()
    where host_id = v_uid
      and status = 'live'
      and id <> p_room_id;

    update public.echo_room_participants p
    set left_at = now(),
        hand_raised = false
    where p.user_id = v_uid
      and p.left_at is null
      and p.room_id <> p_room_id;

    insert into public.echo_room_participants (room_id, user_id, role, joined_at, last_seen_at)
    values (p_room_id, v_uid, 'audience', now(), now())
    on conflict (room_id, user_id) do update
        set left_at = null,
            joined_at = now(),
            last_seen_at = now(),
            hand_raised = false
    returning role into v_role;

    return query select v_role;
end;
$$;

revoke all on function public.join_echo_room(uuid) from public, anon;
grant execute on function public.join_echo_room(uuid) to authenticated;

create or replace function public.leave_echo_room(p_room_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_host_id text;
    v_status text;
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;

    select r.host_id, r.status into v_host_id, v_status
    from public.echo_rooms r
    where r.id = p_room_id;

    if not found then
        return;
    end if;

    update public.echo_room_participants
    set left_at = now(),
        hand_raised = false
    where room_id = p_room_id
      and user_id = v_uid
      and left_at is null;

    if v_host_id = v_uid and v_status = 'live' then
        update public.echo_rooms
        set status = 'ended',
            ended_at = now()
        where id = p_room_id;
    end if;
end;
$$;

revoke all on function public.leave_echo_room(uuid) from public, anon;
grant execute on function public.leave_echo_room(uuid) to authenticated;

create or replace function public.echo_room_heartbeat(p_room_id uuid)
returns table (status text)
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_status text;
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;

    update public.echo_room_participants
    set last_seen_at = now()
    where room_id = p_room_id
      and user_id = v_uid
      and left_at is null;

    if not found then
        raise exception 'SYNAPSE_ECHO_NOT_MEMBER' using errcode = '42501';
    end if;

    select r.status into v_status from public.echo_rooms r where r.id = p_room_id;
    if not found then
        raise exception 'SYNAPSE_ECHO_NOT_FOUND' using errcode = 'P0002';
    end if;

    return query select v_status;
end;
$$;

revoke all on function public.echo_room_heartbeat(uuid) from public, anon;
grant execute on function public.echo_room_heartbeat(uuid) to authenticated;

create or replace function public.set_echo_hand_raised(p_room_id uuid, p_raised boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;

    update public.echo_room_participants
    set hand_raised = coalesce(p_raised, false)
    where room_id = p_room_id
      and user_id = v_uid
      and left_at is null
      and role = 'audience';

    if not found then
        raise exception 'SYNAPSE_ECHO_NOT_AUDIENCE' using errcode = '42501';
    end if;
end;
$$;

revoke all on function public.set_echo_hand_raised(uuid, boolean) from public, anon;
grant execute on function public.set_echo_hand_raised(uuid, boolean) to authenticated;

create or replace function public.set_echo_participant_role(
    p_room_id uuid,
    p_user_id text,
    p_role text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_room public.echo_rooms;
    v_speakers integer;
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;
    if p_role not in ('speaker', 'audience') then
        raise exception 'SYNAPSE_ECHO_INVALID_ROLE' using errcode = '22023';
    end if;

    select * into v_room from public.echo_rooms r where r.id = p_room_id;
    if not found then
        raise exception 'SYNAPSE_ECHO_NOT_FOUND' using errcode = 'P0002';
    end if;
    if v_room.host_id <> v_uid then
        raise exception 'SYNAPSE_ECHO_FORBIDDEN' using errcode = '42501';
    end if;
    if v_room.status <> 'live' then
        raise exception 'SYNAPSE_ECHO_ENDED' using errcode = '55000';
    end if;
    if p_user_id = v_room.host_id then
        raise exception 'SYNAPSE_ECHO_INVALID_TARGET' using errcode = '22023';
    end if;

    if p_role = 'speaker' then
        select count(*) into v_speakers
        from public.echo_room_participants p
        where p.room_id = p_room_id
          and p.left_at is null
          and p.role in ('host', 'speaker');

        if v_speakers >= v_room.max_speakers then
            raise exception 'SYNAPSE_ECHO_STAGE_FULL' using errcode = '23505';
        end if;
    end if;

    update public.echo_room_participants
    set role = p_role,
        hand_raised = false
    where room_id = p_room_id
      and user_id = p_user_id
      and left_at is null;

    if not found then
        raise exception 'SYNAPSE_ECHO_NOT_MEMBER' using errcode = 'P0002';
    end if;
end;
$$;

revoke all on function public.set_echo_participant_role(uuid, text, text) from public, anon;
grant execute on function public.set_echo_participant_role(uuid, text, text) to authenticated;

create or replace function public.end_echo_room(p_room_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_host_id text;
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;

    select r.host_id into v_host_id from public.echo_rooms r where r.id = p_room_id;
    if not found then
        raise exception 'SYNAPSE_ECHO_NOT_FOUND' using errcode = 'P0002';
    end if;
    if v_host_id <> v_uid then
        raise exception 'SYNAPSE_ECHO_FORBIDDEN' using errcode = '42501';
    end if;

    update public.echo_rooms
    set status = 'ended',
        ended_at = now()
    where id = p_room_id
      and status = 'live';
end;
$$;

revoke all on function public.end_echo_room(uuid) from public, anon;
grant execute on function public.end_echo_room(uuid) to authenticated;

create or replace function public.get_echo_lobby(p_limit integer default 30)
returns table (
    id uuid,
    host_id text,
    title text,
    topic text,
    visibility text,
    max_speakers integer,
    created_at timestamptz,
    host_username text,
    host_display_name text,
    host_avatar text,
    speaker_count integer,
    listener_count integer,
    followed_in_room jsonb
)
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;

    perform public.expire_echo_rooms();

    return query
    select
        r.id,
        r.host_id,
        r.title,
        r.topic,
        r.visibility,
        r.max_speakers,
        r.created_at,
        u.username,
        u.display_name,
        u.avatar,
        coalesce((
            select count(*) from public.echo_room_participants p
            where p.room_id = r.id
              and p.left_at is null
              and p.last_seen_at > now() - interval '2 minutes'
              and p.role in ('host', 'speaker')
        ), 0)::integer as speaker_count,
        coalesce((
            select count(*) from public.echo_room_participants p
            where p.room_id = r.id
              and p.left_at is null
              and p.last_seen_at > now() - interval '2 minutes'
              and p.role = 'audience'
        ), 0)::integer as listener_count,
        coalesce((
            select jsonb_agg(jsonb_build_object(
                'user_id', followed.user_id,
                'username', followed.username,
                'avatar', followed.avatar
            ))
            from (
                select p.user_id, fu.username, fu.avatar
                from public.echo_room_participants p
                join public.users fu on fu.uid = p.user_id
                where p.room_id = r.id
                  and p.left_at is null
                  and p.last_seen_at > now() - interval '2 minutes'
                  and exists (
                      select 1 from public.follows f
                      where f.follower_id = v_uid
                        and f.following_id = p.user_id
                  )
                limit 3
            ) followed
        ), '[]'::jsonb) as followed_in_room
    from public.echo_rooms r
    join public.users u on u.uid = r.host_id
    where r.status = 'live'
      and r.visibility = 'public'
      and coalesce(u.banned, false) = false
      and not exists (
          select 1 from public.blocks b
          where (b.blocker_id = v_uid and b.blocked_id = r.host_id)
             or (b.blocker_id = r.host_id and b.blocked_id = v_uid)
      )
    order by r.created_at desc
    limit greatest(1, least(coalesce(p_limit, 30), 50));
end;
$$;

revoke all on function public.get_echo_lobby(integer) from public, anon;
grant execute on function public.get_echo_lobby(integer) to authenticated;

create or replace function public.get_echo_room(p_room_id uuid)
returns table (
    id uuid,
    host_id text,
    title text,
    topic text,
    visibility text,
    status text,
    max_speakers integer,
    created_at timestamptz,
    ended_at timestamptz,
    host_username text,
    host_display_name text,
    host_avatar text,
    my_role text,
    participants jsonb
)
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_room public.echo_rooms;
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;

    select * into v_room from public.echo_rooms r where r.id = p_room_id;
    if not found then
        raise exception 'SYNAPSE_ECHO_NOT_FOUND' using errcode = 'P0002';
    end if;

    if v_room.host_id <> v_uid and not public.synapse_is_echo_member(p_room_id) then
        raise exception 'SYNAPSE_ECHO_FORBIDDEN' using errcode = '42501';
    end if;

    return query
    select
        r.id,
        r.host_id,
        r.title,
        r.topic,
        r.visibility,
        r.status,
        r.max_speakers,
        r.created_at,
        r.ended_at,
        u.username,
        u.display_name,
        u.avatar,
        (
            select p.role from public.echo_room_participants p
            where p.room_id = r.id
              and p.user_id = v_uid
              and p.left_at is null
        ) as my_role,
        coalesce((
            select jsonb_agg(jsonb_build_object(
                'user_id', p.user_id,
                'role', p.role,
                'hand_raised', p.hand_raised,
                'joined_at', p.joined_at,
                'username', pu.username,
                'display_name', pu.display_name,
                'avatar', pu.avatar
            ) order by
                case p.role when 'host' then 0 when 'speaker' then 1 else 2 end,
                p.joined_at)
            from public.echo_room_participants p
            join public.users pu on pu.uid = p.user_id
            where p.room_id = r.id
              and p.left_at is null
              and p.last_seen_at > now() - interval '2 minutes'
        ), '[]'::jsonb) as participants
    from public.echo_rooms r
    join public.users u on u.uid = r.host_id
    where r.id = p_room_id;
end;
$$;

revoke all on function public.get_echo_room(uuid) from public, anon;
grant execute on function public.get_echo_room(uuid) to authenticated;

-- The Edge Function trusts only this function's answer: it resolves the caller inside Postgres,
-- so a client cannot ask for a publisher grant it does not have.
create or replace function public.get_echo_token_context(p_room_id uuid)
returns table (
    user_id text,
    livekit_room text,
    role text,
    display_name text
)
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
begin
    if auth.uid() is null or v_uid is null then
        raise exception 'SYNAPSE_ECHO_UNAUTHENTICATED' using errcode = '42501';
    end if;

    return query
    select
        p.user_id,
        r.livekit_room,
        p.role,
        coalesce(nullif(btrim(u.display_name), ''), nullif(btrim(u.username), ''), 'Listener')
    from public.echo_rooms r
    join public.echo_room_participants p
      on p.room_id = r.id
     and p.user_id = v_uid
     and p.left_at is null
    join public.users u on u.uid = p.user_id
    where r.id = p_room_id
      and r.status = 'live';

    if not found then
        raise exception 'SYNAPSE_ECHO_FORBIDDEN' using errcode = '42501';
    end if;
end;
$$;

revoke all on function public.get_echo_token_context(uuid) from public, anon;
grant execute on function public.get_echo_token_context(uuid) to authenticated;

do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'echo_rooms'
    ) then
        alter publication supabase_realtime add table public.echo_rooms;
    end if;

    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'echo_room_participants'
    ) then
        alter publication supabase_realtime add table public.echo_room_participants;
    end if;
end;
$$;
