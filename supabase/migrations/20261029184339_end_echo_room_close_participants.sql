-- end_echo_room flipped the room to 'ended' but left every participant row open, so a host who
-- ended their own room stayed an "active" member of it until expire_echo_rooms() happened to run.
-- Ending a room disconnects everyone, so close the roster in the same transaction.
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

    update public.echo_room_participants
    set left_at = now(),
        hand_raised = false
    where room_id = p_room_id
      and left_at is null;
end;
$$;

revoke all on function public.end_echo_room(uuid) from public, anon;
grant execute on function public.end_echo_room(uuid) to authenticated;
