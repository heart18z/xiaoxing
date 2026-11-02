package org.springblade.modules.smartreminder.push;

import org.springframework.jdbc.core.JdbcTemplate;

public final class LiveActivitySchema {
    private LiveActivitySchema() {}
    public static void migrate(JdbcTemplate jdbc) {
        jdbc.execute("""
            create table if not exists blade_smart_live_device (
              device_id varchar(36) primary key, binding_id varchar(36) not null,
              start_token text not null, enabled tinyint not null default 1,
              lease_until datetime(6) null, lease_token varchar(36) null, updated_at datetime(6) not null
            )
            """);
        jdbc.execute("""
            create table if not exists blade_smart_live_activity (
              id varchar(36) primary key, device_id varchar(36) not null, binding_id varchar(36) not null,
              activity_key varchar(40) not null, user_id bigint not null, environment varchar(20) not null,
              activity_token text not null, status varchar(20) not null,
              content_json text not null, content_hash varchar(64) not null,
              expires_at datetime(6) not null, updated_at datetime(6) not null, last_error varchar(100) null,
              unique key uk_live_window(device_id,binding_id,activity_key), key idx_live_state(status,expires_at)
            )
            """);
    }
}
