package org.springblade.modules.smartreminder.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class RegistrationMailSchema {
    private final JdbcTemplate jdbc;
    @EventListener(ApplicationReadyEvent.class)
    public void migrate(){
        jdbc.execute("create table if not exists blade_registration_mail(id int primary key,host varchar(253) not null,port int not null,security varchar(20) not null,username varchar(45) not null,password_cipher text not null,from_address varchar(45) not null,from_name varchar(60) not null,enabled tinyint not null default 0)");
        jdbc.execute("create table if not exists blade_registration_code(email varchar(45) primary key,nonce varchar(36) not null,code_hash char(64) not null,expires_at bigint not null,attempts int not null default 0,delivered tinyint not null default 0,used tinyint not null default 0)");
        jdbc.execute("create table if not exists blade_registration_rate(bucket char(64) primary key,started_at bigint not null,attempts int not null)");
    }
}
