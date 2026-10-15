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
        Boolean hasChannel = jdbc.query("select * from blade_app_download_page where 1=0", rs -> {
            var meta=rs.getMetaData();
            for(int i=1;i<=meta.getColumnCount();i++)
                if("harmony_enabled".equalsIgnoreCase(meta.getColumnName(i)))return true;
            return false;
        });
        if(!Boolean.TRUE.equals(hasChannel))
            jdbc.execute("alter table blade_app_download_page add column harmony_enabled tinyint not null default 1");
    }
}
