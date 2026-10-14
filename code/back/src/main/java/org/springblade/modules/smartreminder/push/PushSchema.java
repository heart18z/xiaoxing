package org.springblade.modules.smartreminder.push;

import org.springframework.jdbc.core.JdbcTemplate;

public final class PushSchema {
    private PushSchema() {}
    public static void migrate(JdbcTemplate jdbc) {
        jdbc.execute("""
            create table if not exists blade_smart_push_device (
              id varchar(36) primary key, user_id bigint not null,
              secret_hash varchar(64) not null, token_hash varchar(64) null,
              device_token text not null, environment varchar(20) not null,
              binding_id varchar(36) not null, enabled tinyint not null default 1,
              language varchar(10) not null default 'zh-cn', app_version varchar(40) null,
              updated_at datetime(6) not null,
              unique key uk_push_token(environment,token_hash), key idx_push_user(user_id,enabled)
            )
            """);
        jdbc.execute("""
            create table if not exists blade_smart_push_delivery (
              id bigint primary key, message_id bigint not null, event_id bigint null,
              user_id bigint not null, device_id varchar(36) not null, binding_id varchar(36) not null,
              apns_id varchar(36) not null, status varchar(20) not null default 'PENDING',
              attempts int not null default 0, next_attempt_at datetime(6) not null,
              expires_at datetime(6) not null, lease_token varchar(36) null, lease_until datetime(6) null,
              last_error varchar(100) null, sent_at datetime(6) null, created_at datetime(6) not null,
              unique key uk_push_message_device(message_id,device_id,binding_id),
              key idx_push_pending(status,next_attempt_at), key idx_push_device(device_id,binding_id)
            )
            """);
        // Existing installations created this column as NOT NULL. Social notifications have no event.
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            try (var columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "blade_smart_push_delivery", "event_id")) {
                if (columns.next() && columns.getInt("NULLABLE") == java.sql.DatabaseMetaData.columnNoNulls) {
                    try (var statement = connection.createStatement()) { statement.execute("alter table blade_smart_push_delivery modify column event_id bigint null"); }
                }
            }
            return null;
        });
    }
}
