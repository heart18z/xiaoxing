-- AI小醒自定义群组：仅新增群组、成员、标签和成员标签关系，不扩大好友提醒权限。
SET NAMES utf8mb4;

create table if not exists blade_reminder_group (
              id bigint primary key, owner_user_id bigint not null, name varchar(60) not null,
              avatar varchar(1000) null, introduction varchar(500) null,
              group_type varchar(20) not null default 'CUSTOM', status varchar(20) not null default 'ACTIVE',
              revision bigint not null default 1, create_time datetime not null default current_timestamp,
              update_time datetime not null default current_timestamp, key idx_group_owner(owner_user_id,status)
            );

create table if not exists blade_reminder_group_member (
              group_id bigint not null, user_id bigint not null, member_role varchar(20) not null default 'MEMBER',
              status varchar(20) not null default 'ACTIVE', joined_at datetime not null default current_timestamp,
              primary key(group_id,user_id), key idx_group_member_user(user_id,status)
            );

create table if not exists blade_reminder_group_tag (
              id bigint primary key, group_id bigint not null, name varchar(40) not null,
              unique key uk_group_tag_name(group_id,name), key idx_group_tag(group_id)
            );

create table if not exists blade_reminder_group_member_tag (
              group_id bigint not null, user_id bigint not null, tag_id bigint not null,
              primary key(group_id,user_id,tag_id), key idx_group_tag_members(group_id,tag_id)
            );
