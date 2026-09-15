package org.springblade.modules.smartreminder.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SmartSocialService {
    private final JdbcTemplate jdbc;

    @Transactional(rollbackFor=Exception.class)
    public void saveRemark(Long owner, Long person, String remark) {
        if (person == null || person.equals(owner) || remark == null)
            throw new ServiceException("请选择好友并填写备注，留空可清除备注");
        String value = remark.trim();
        if (value.codePointCount(0, value.length()) > 30 || value.chars().anyMatch(Character::isISOControl))
            throw new ServiceException("好友备注不能超过30字，不能包含换行或控制字符");
        // The caller's identity comes only from authentication, never a client-supplied owner.
        List<Map<String,Object>> rows = jdbc.queryForList(
            "select id from blade_friendship where owner_user_id=? and friend_user_id=? and status='ACTIVE' for update", owner, person);
        if (rows.isEmpty()) throw new ServiceException("只能修改自己的现有好友备注");
        jdbc.update("update blade_friendship set friend_remark=?,update_time=now() where owner_user_id=? and friend_user_id=? and status='ACTIVE'",
            value.isEmpty() ? null : value, owner, person);
    }

    @Transactional(rollbackFor=Exception.class)
    public void rememberRemarks(Long owner, JsonNode updates) {
        if (!updates.isArray()) return;
        if (updates.size() > 20) throw new ServiceException("一次最多设置20个好友备注");
        for (JsonNode item : updates) {
            Long person;
            try { person = Long.valueOf(item.path("personUserId").asText()); }
            catch (Exception e) { throw new ServiceException("请先确认备注对应哪位好友"); }
            if (!item.path("remark").isTextual()) throw new ServiceException("请提供好友备注");
            saveRemark(owner, person, item.path("remark").asText());
        }
    }

    public List<Map<String,Object>> aliases(Long owner) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
            select a.alias_name as aliasName,a.person_user_id as personUserId,u.account,u.name
            from blade_smart_person_alias a join blade_user u on u.id=a.person_user_id and u.is_deleted=0
            join blade_friendship f on f.owner_user_id=a.owner_user_id and f.friend_user_id=a.person_user_id and f.status='ACTIVE'
            where a.owner_user_id=? order by a.update_time desc
            """,owner);
        rows.forEach(row->row.put("personUserId",row.get("personUserId").toString()));return rows;
    }

    public void remember(Long owner,Long source,JsonNode updates) {
        if(!updates.isArray())return;
        for(JsonNode item:updates) {
            String alias=item.path("alias").asText("").trim();
            Long person;
            try {person=Long.valueOf(item.path("personUserId").asText());}catch(Exception e){throw new ServiceException("别称尚未关联到确定的好友，请先确认对应的人。");}
            if(alias.isBlank()||alias.length()>100)throw new ServiceException("别称不能为空且不能超过100字");
            if(jdbc.queryForObject("select count(*) from blade_friendship where owner_user_id=? and friend_user_id=? and status='ACTIVE'",Integer.class,owner,person)==0)
                throw new ServiceException("只能记录当前好友的别称，不能把未知称呼随意绑定给其他人。");
            jdbc.update("""
                insert into blade_smart_person_alias(id,owner_user_id,person_user_id,alias_name,source_message_id,create_time,update_time)
                values(?,?,?,?,?,now(),now()) on duplicate key update person_user_id=values(person_user_id),source_message_id=values(source_message_id),update_time=now()
                """,IdWorker.getId(),owner,person,alias,source);
        }
    }

    @Transactional(rollbackFor=Exception.class)
    public void removeFriend(Long owner,Long person) {
        if(person==null||person.equals(owner))throw new ServiceException("请选择需要解除关系的好友");
        // Both directions and pending permission changes must change together; retain historical events and messages.
        jdbc.queryForList("select id from blade_user where id in (?,?) order by id for update",owner,person);
        jdbc.update("update blade_friendship set status='REMOVED',update_time=now() where (owner_user_id=? and friend_user_id=?) or (owner_user_id=? and friend_user_id=?)",owner,person,person,owner);
        jdbc.update("update blade_friend_request set request_status='CANCELLED',update_time=now() where request_status='PENDING' and ((applicant_user_id=? and target_user_id=?) or (applicant_user_id=? and target_user_id=?))",owner,person,person,owner);
    }

    public String aiAvatar(Long user) {
        List<String> values=jdbc.query("select ai_avatar from blade_smart_user_preference where user_id=?",(rs,n)->rs.getString(1),user);
        return values.isEmpty()||values.get(0)==null?"":values.get(0);
    }
    public void saveAiAvatar(Long user,String avatar) {
        if(avatar==null)return;
        validateAvatar(avatar);
        jdbc.update("insert into blade_smart_user_preference(user_id,ai_avatar,update_time) values(?,?,now()) on duplicate key update ai_avatar=values(ai_avatar),update_time=now()",user,avatar);
    }
    public static void validateAvatar(String avatar) {
        if(avatar.length()>1000||(!avatar.isBlank()&&!avatar.startsWith("/")&&!avatar.startsWith("https://")&&!avatar.startsWith("http://")))throw new ServiceException("请使用默认头像或上传图片");
    }
}
