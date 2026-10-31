-- Group invites, join requests and admin member management.
--
-- The client has shipped these flows (create-group settings, invite link + QR, pending
-- requests, promote/demote/remove, disappearing mode) against a backend that was never
-- provisioned: chats lacked the settings columns and invite_code, group_join_requests did
-- not exist, and every group-management RPC was missing. The create-group insert therefore
-- silently fell back to a minimal row that dropped the description and permissions.
--
-- Identity is the app uid (users.uid, text), matching chat_participants and get_current_user_uid().

-- ---------------------------------------------------------------------------------------------
-- chats: group settings + revocable invite code
-- ---------------------------------------------------------------------------------------------

alter table public.chats
    add column if not exists allow_member_settings_edits boolean not null default false,
    add column if not exists require_join_approval boolean not null default true,
    add column if not exists allow_member_invites boolean not null default false;

create or replace function public.generate_group_invite_code()
returns text
language sql
volatile
set search_path = public
as $$
    select substr(md5(gen_random_uuid()::text), 1, 16);
$$;

alter table public.chats
    add column if not exists invite_code text;

update public.chats
set invite_code = public.generate_group_invite_code()
where invite_code is null;

alter table public.chats
    alter column invite_code set default public.generate_group_invite_code();

create unique index if not exists chats_invite_code_key
    on public.chats (invite_code)
    where invite_code is not null;

-- ---------------------------------------------------------------------------------------------
-- group_join_requests: pending membership applications from invite links
-- ---------------------------------------------------------------------------------------------

create table if not exists public.group_join_requests (
    id uuid primary key default gen_random_uuid(),
    chat_id uuid not null references public.chats(id) on delete cascade,
    user_id text not null references public.users(uid) on delete cascade,
    status text not null default 'pending',
    created_at timestamptz not null default now(),
    constraint group_join_requests_status check (status in ('pending', 'approved', 'declined'))
);

-- One open request per user per group; decline/approve history stays queryable.
create unique index if not exists group_join_requests_one_pending
    on public.group_join_requests (chat_id, user_id)
    where status = 'pending';

create index if not exists group_join_requests_chat_status_idx
    on public.group_join_requests (chat_id, status, created_at desc);

alter table public.group_join_requests enable row level security;

create policy group_join_requests_select_member_or_admin
    on public.group_join_requests
    for select
    to authenticated
    using (
        user_id = public.get_current_user_uid()
        or private.is_chat_admin(chat_id, public.get_current_user_uid())
    );

-- All writes go through the SECURITY DEFINER RPCs below.
revoke all on table public.group_join_requests from anon, authenticated;
grant select on table public.group_join_requests to authenticated;

-- ---------------------------------------------------------------------------------------------
-- Invite link RPCs
-- ---------------------------------------------------------------------------------------------

create or replace function public.rotate_group_invite_code(p_chat_id uuid)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_code text;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '28000';
    end if;

    if not private.is_chat_admin(p_chat_id, v_uid) then
        raise exception 'only group admins can replace the invite link' using errcode = '42501';
    end if;

    v_code := public.generate_group_invite_code();

    update public.chats
    set invite_code = v_code,
        updated_at = now()
    where id = p_chat_id;

    if not found then
        raise exception 'group not found' using errcode = 'P0002';
    end if;

    return v_code;
end;
$$;

create or replace function public.resolve_group_invite(p_invite_code text)
returns table (
    chat_id uuid,
    name text,
    description text,
    avatar_url text,
    require_join_approval boolean,
    member_count bigint
)
language sql
stable
security definer
set search_path = public
as $$
    select
        c.id,
        coalesce(c.name, ''),
        c.description,
        c.avatar_url,
        coalesce(c.require_join_approval, true),
        (select count(*) from public.chat_participants cp where cp.chat_id = c.id)
    from public.chats c
    where c.invite_code = p_invite_code
      and c.is_group is true
    limit 1;
$$;

create or replace function public.request_group_join_by_invite(p_invite_code text)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_chat public.chats;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '28000';
    end if;

    select * into v_chat
    from public.chats
    where invite_code = p_invite_code
      and is_group is true;

    if not found then
        raise exception 'invite not found' using errcode = 'P0002';
    end if;

    if exists (
        select 1 from public.chat_participants
        where chat_id = v_chat.id and user_id = v_uid
    ) then
        return 'already_member';
    end if;

    if coalesce(v_chat.require_join_approval, true) then
        insert into public.group_join_requests (chat_id, user_id)
        values (v_chat.id, v_uid)
        on conflict do nothing;
        return 'pending';
    end if;

    insert into public.chat_participants (chat_id, user_id)
    select v_chat.id, v_uid
    where not exists (
        select 1 from public.chat_participants
        where chat_id = v_chat.id and user_id = v_uid
    );
    return 'joined';
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Join request review RPCs
-- ---------------------------------------------------------------------------------------------

create or replace function public.approve_group_join_request(p_request_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_request public.group_join_requests;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '28000';
    end if;

    select * into v_request from public.group_join_requests where id = p_request_id;
    if not found then
        raise exception 'join request not found' using errcode = 'P0002';
    end if;

    if not private.is_chat_admin(v_request.chat_id, v_uid) then
        raise exception 'only group admins can approve requests' using errcode = '42501';
    end if;

    update public.group_join_requests
    set status = 'approved'
    where id = p_request_id;

    insert into public.chat_participants (chat_id, user_id)
    select v_request.chat_id, v_request.user_id
    where not exists (
        select 1 from public.chat_participants
        where chat_id = v_request.chat_id and user_id = v_request.user_id
    );
end;
$$;

create or replace function public.decline_group_join_request(p_request_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_request public.group_join_requests;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '28000';
    end if;

    select * into v_request from public.group_join_requests where id = p_request_id;
    if not found then
        raise exception 'join request not found' using errcode = 'P0002';
    end if;

    if not private.is_chat_admin(v_request.chat_id, v_uid) then
        raise exception 'only group admins can decline requests' using errcode = '42501';
    end if;

    update public.group_join_requests
    set status = 'declined'
    where id = p_request_id;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Member management RPCs
-- ---------------------------------------------------------------------------------------------

create or replace function public.remove_group_member(p_chat_id uuid, p_user_id text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_created_by text;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '28000';
    end if;

    if not private.is_chat_admin(p_chat_id, v_uid) then
        raise exception 'only group admins can remove members' using errcode = '42501';
    end if;

    select created_by into v_created_by from public.chats where id = p_chat_id;
    if v_created_by = p_user_id then
        raise exception 'the group creator cannot be removed' using errcode = '42501';
    end if;

    delete from public.chat_participants
    where chat_id = p_chat_id
      and user_id = p_user_id;
end;
$$;

create or replace function public.set_group_member_admin(p_chat_id uuid, p_user_id text, p_is_admin boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
    v_created_by text;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '28000';
    end if;

    if not private.is_chat_admin(p_chat_id, v_uid) then
        raise exception 'only group admins can change roles' using errcode = '42501';
    end if;

    select created_by into v_created_by from public.chats where id = p_chat_id;
    if v_created_by = p_user_id then
        raise exception 'the group creator is always an admin' using errcode = '42501';
    end if;

    update public.chat_participants
    set is_admin = p_is_admin
    where chat_id = p_chat_id
      and user_id = p_user_id;

    if not found then
        raise exception 'member not found' using errcode = 'P0002';
    end if;
end;
$$;

create or replace function public.update_group_message_permission(p_chat_id uuid, p_only_admins_can_message boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '28000';
    end if;

    if not private.is_chat_admin(p_chat_id, v_uid) then
        raise exception 'only group admins can change message permissions' using errcode = '42501';
    end if;

    update public.chats
    set only_admins_can_message = p_only_admins_can_message,
        updated_at = now()
    where id = p_chat_id;
end;
$$;

create or replace function public.update_group_disappearing_mode(p_chat_id uuid, p_disappearing_mode text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text := public.get_current_user_uid();
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode = '28000';
    end if;

    if not private.is_chat_admin(p_chat_id, v_uid) then
        raise exception 'only group admins can change disappearing messages' using errcode = '42501';
    end if;

    if p_disappearing_mode not in ('OFF', 'TWENTY_FOUR_HOURS', 'SEVEN_DAYS', 'NINETY_DAYS') then
        raise exception 'unsupported disappearing mode' using errcode = '22023';
    end if;

    update public.chats
    set disappearing_mode = p_disappearing_mode,
        updated_at = now()
    where id = p_chat_id;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Grants
-- ---------------------------------------------------------------------------------------------

revoke all on function public.generate_group_invite_code() from public, anon;
revoke all on function public.rotate_group_invite_code(uuid) from public, anon;
revoke all on function public.resolve_group_invite(text) from public, anon;
revoke all on function public.request_group_join_by_invite(text) from public, anon;
revoke all on function public.approve_group_join_request(uuid) from public, anon;
revoke all on function public.decline_group_join_request(uuid) from public, anon;
revoke all on function public.remove_group_member(uuid, text) from public, anon;
revoke all on function public.set_group_member_admin(uuid, text, boolean) from public, anon;
revoke all on function public.update_group_message_permission(uuid, boolean) from public, anon;
revoke all on function public.update_group_disappearing_mode(uuid, text) from public, anon;

grant execute on function public.generate_group_invite_code() to authenticated, service_role;
grant execute on function public.rotate_group_invite_code(uuid) to authenticated, service_role;
grant execute on function public.resolve_group_invite(text) to authenticated, service_role;
grant execute on function public.request_group_join_by_invite(text) to authenticated, service_role;
grant execute on function public.approve_group_join_request(uuid) to authenticated, service_role;
grant execute on function public.decline_group_join_request(uuid) to authenticated, service_role;
grant execute on function public.remove_group_member(uuid, text) to authenticated, service_role;
grant execute on function public.set_group_member_admin(uuid, text, boolean) to authenticated, service_role;
grant execute on function public.update_group_message_permission(uuid, boolean) to authenticated, service_role;
grant execute on function public.update_group_disappearing_mode(uuid, text) to authenticated, service_role;

notify pgrst, 'reload schema';
