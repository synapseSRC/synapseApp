-- join_echo_room declares an OUT column named "role" (the RPC response field the client reads),
-- which also matches echo_room_participants.role. The bare `returning role` therefore failed with
-- SQLSTATE 42702 ("column reference \"role\" is ambiguous"), breaking every room join.
-- Alias the insert target so the returned column is unambiguously the table's.
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

    insert into public.echo_room_participants as participant (room_id, user_id, role, joined_at, last_seen_at)
    values (p_room_id, v_uid, 'audience', now(), now())
    on conflict (room_id, user_id) do update
        set left_at = null,
            joined_at = now(),
            last_seen_at = now(),
            hand_raised = false
    returning participant.role into v_role;

    return query select v_role;
end;
$$;

revoke all on function public.join_echo_room(uuid) from public, anon;
grant execute on function public.join_echo_room(uuid) to authenticated;
