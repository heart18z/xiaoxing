package org.springblade.modules.smartreminder.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportSchema {
    private final JdbcTemplate db;
    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        db.execute("""
            create table if not exists blade_smart_report (
              id bigint primary key,user_id bigint not null,period_start date not null,period_end date not null,
              source varchar(20) not null,generation_key varchar(160) not null unique,
              status varchar(20) not null,payload_json longtext null,error_message varchar(500) null,
              lease_token varchar(40) null,lease_until datetime null,attempts int not null default 0,
              create_time datetime not null default current_timestamp,update_time datetime not null default current_timestamp,
              sent_time datetime null, key idx_report_owner(user_id,period_start),key idx_report_status(status,lease_until)
            )
            """);
        db.execute("""
            create table if not exists blade_smart_report_setting (
              user_id bigint primary key,enabled tinyint not null default 1,
              send_day int not null default 1,send_hour int not null default 9,
              update_time datetime not null default current_timestamp
            )
            """);
        db.update("insert ignore into blade_smart_report_setting(user_id,enabled,send_day,send_hour) values(0,1,1,9)");
        db.execute("""
            create table if not exists blade_smart_report_delivery (
              id bigint primary key,report_id bigint not null,request_key varchar(100) not null,
              actor_id bigint null,message_id bigint not null,create_time datetime not null default current_timestamp,
              unique key uk_report_delivery(report_id,request_key)
            )
            """);
        // Mirror the existing report administrators; no APP role receives management access.
        db.update("""
            insert into blade_menu(id,parent_id,code,name,alias,path,source,sort,category,action,is_open,component,remark,is_deleted)
            select 2099000000000000120,2099000000000000090,'smartReminderReports','事件周报','menu',
              '/smartReminder/reports','iconfont iconicon_savememo',6,1,0,1,null,'个人周报与发送管理',0
            where not exists(select 1 from blade_menu where id=2099000000000000120)
            """);
        var roles=db.queryForList("""
            select r.role_id from
              (select distinct role_id from blade_role_menu where menu_id=2099000000000000101) r
            where not exists(select 1 from blade_role_menu m where m.role_id=r.role_id and m.menu_id=2099000000000000120)
            """);
        for(var role:roles)db.update("insert into blade_role_menu(id,role_id,menu_id) values(?,?,2099000000000000120)",com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(),role.get("role_id"));
    }
}
