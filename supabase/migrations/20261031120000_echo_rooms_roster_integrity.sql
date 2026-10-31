-- Roster integrity fixes for Echo Rooms.
--
-- 1. leave_echo_room: a host leaving ends the room but only closed the host's own row, so every
--    other member stayed "in" a dead room (still passing synapse_is_echo_member, reading it via
--    get_echo_room and heartbeating) until expire_echo_rooms() happened to run. Close the whole
--    roster, as end_echo_room already does.
-- 2. join_echo_room: joining another room ends the caller's own live room with the same gap, so
--    that roster is closed too. A member who explicitly left and comes back now rejoins as
--    audience; previously they silently kept a speaker seat, bypassing max_speakers. An open row
--    (a reconnect after a dropped session) keeps its role.
-- 3. set_echo_participant_role: the stage count included members who vanished without leaving
--    (no heartbeat for 2 minutes) and the target themself, so a host could hit STAGE_FULL with
--    free seats, or when re-confirming an existing speaker.

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

    if v_host_id = v_uid and v_status = 'live' then
        update public.echo_rooms
        set status = 'ended',
            ended_at = now()
        where id = p_room_id;

        update public.echo_room_participants
        set left_at = now(),
            hand_raised = false
        where room_id = p_room_id
          and left_at is null;
        return;
    end if;

    update public.echo_room_participants
    set left_at = now(),
        hand_raised = false
    where room_id = p_room_id
      and user_id = v_uid
      and left_at is null;
end;
$$;

revoke all on function public.leave_echo_room(uuid) from public, anon;
grant execute on function public.leave_echo_room(uuid) to authenticated;

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

    -- One audio room at a time per account. Ending the caller's own room closes its whole roster,
    -- and the caller's memberships elsewhere are closed alongside.
    with ended as (
        update public.echo_rooms r
        set status = 'ended',
            ended_at = now()
        where r.host_id = v_uid
          and r.status = 'live'
          and r.id <> p_room_id
        returning r.id
    )
    update public.echo_room_participants p
    set left_at = now(),
        hand_raised = false
    where p.left_at is null
      and p.room_id <> p_room_id
      and (p.user_id = v_uid or p.room_id in (select ended.id from ended));

    insert into public.echo_room_participants as participant (room_id, user_id, role, joined_at, last_seen_at)
    values (p_room_id, v_uid, 'audience', now(), now())
    on conflict (room_id, user_id) do update
        set role = case
                when participant.left_at is null or participant.role = 'host' then participant.role
                else 'audience'
            end,
            left_at = null,
            joined_at = now(),
            last_seen_at = now(),
            hand_raised = false
    returning participant.role into v_role;

    return query select v_role;
end;
$$;

revoke all on function public.join_echo_room(uuid) from public, anon;
grant execute on function public.join_echo_room(uuid) to authenticated;

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
          and p.last_seen_at > now() - interval '2 minutes'
          and p.role in ('host', 'speaker')
          and p.user_id <> p_user_id;

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
