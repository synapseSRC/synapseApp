-- my_role was derived only from an open participant row, so once a room ended (end_echo_room now
-- closes the roster) the host read back as a stranger and the room screen could not tell them
-- their own room had ended. Hostship is a property of the room, not of an active membership.
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
    viewer_id text,
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
        v_uid,
        coalesce(
            (
                select p.role from public.echo_room_participants p
                where p.room_id = r.id
                  and p.user_id = v_uid
                  and p.left_at is null
            ),
            case when r.host_id = v_uid then 'host' end
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
