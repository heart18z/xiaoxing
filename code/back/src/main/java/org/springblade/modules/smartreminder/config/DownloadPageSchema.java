package org.springblade.modules.smartreminder.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DownloadPageSchema {
    private final JdbcTemplate jdbc;
    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        jdbc.execute("create table if not exists blade_app_download_page(id int primary key,page_url varchar(1000) not null,android_enabled tinyint not null,ios_enabled tinyint not null,ios_mode varchar(20) not null,app_store_url varchar(1000) not null,testflight_url varchar(1000) not null,harmony_apk_enabled tinyint not null,harmony_native_enabled tinyint not null,harmony_store_url varchar(1000) not null)");
    }
}
