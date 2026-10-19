package org.springblade.modules.smartreminder.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.smartreminder.dto.ReminderGroupDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.*;

/** Group membership organizes recipients; it never grants friendship/reminder permissions. */
@Service
@RequiredArgsConstructor
public class ReminderGroupService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public List<Map<String,Object>> list(Long user) {
        return list(user,true);
    }

    private List<Map<String,Object>> list(Long user,boolean previews) {
        List<Map<String,Object>> groups=jdbc.queryForList("""
            select g.id,g.name,g.avatar,g.introduction,g.group_type as groupType,g.revision,
              g.owner_user_id as ownerUserId,m.member_role as myRole,
              (select count(*) from blade_reminder_group_member x join blade_user u on u.id=x.user_id and u.is_deleted=0
               where x.group_id=g.id and x.status='ACTIVE') as memberCount
            from blade_reminder_group g join blade_reminder_group_member m on m.group_id=g.id
            where m.user_id=? and m.status='ACTIVE' and g.status='ACTIVE'
            order by case m.member_role when 'OWNER' then 0 when 'ADMIN' then 1 else 2 end,g.update_time desc,g.id desc
            """,user);
        groups.forEach(group->{
            ids(group);
            if(previews) {
                var people=jdbc.queryForList("""
                    select u.id,u.name,u.account,u.avatar from blade_reminder_group_member m
                    join blade_user u on u.id=m.user_id and u.is_deleted=0
                    where m.group_id=? and m.status='ACTIVE'
                    order by case m.member_role when 'OWNER' then 0 when 'ADMIN' then 1 else 2 end,m.joined_at,u.id limit 4
                    """,Long.valueOf(group.get("id").toString()));
                people.forEach(this::ids);group.put("memberPreviews",people);
            }
        });return groups;
    }

    public List<Map<String,Object>> directory(Long user) {
        List<Map<String,Object>> result=new ArrayList<>();
        for(var group:list(user,false)) {
            if(!manager(group.get("myRole")))continue;
            var index=new LinkedHashMap<String,Object>();
            for(String field:List.of("id","name","introduction","memberCount"))index.put(field,group.get(field));
            index.put("tags",tags(Long.valueOf(group.get("id").toString())));
            result.add(index);
        }
        return result;
    }

    public Map<String,Object> detail(Long user,Long groupId) {
        Map<String,Object> group=access(user,groupId,false,false);
        ids(group);group.put("members",members(user,groupId));group.put("tags",tags(groupId));return group;
    }

    public List<Map<String,Object>> members(Long user,Long groupId) {
        access(user,groupId,false,false);
        var rows=jdbc.queryForList("""
            select u.id,u.account,u.name,u.real_name as realName,u.avatar,m.member_role as memberRole,
              f.friend_remark as friendRemark,f.permission_mode as permissionMode,
              case when f.friend_user_id is null then 0 else 1 end as isFriend
            from blade_reminder_group_member m join blade_user u on u.id=m.user_id and u.is_deleted=0
            left join blade_friendship f on f.owner_user_id=? and f.friend_user_id=u.id and f.status='ACTIVE'
            where m.group_id=? and m.status='ACTIVE'
            order by case m.member_role when 'OWNER' then 0 when 'ADMIN' then 1 else 2 end,coalesce(u.name,u.real_name,u.account),u.id
            """,user,groupId);
        Map<String,List<Map<String,Object>>> labels=new HashMap<>();
        for(var tag:jdbc.queryForList("""
            select mt.user_id as userId,t.id,t.name from blade_reminder_group_member_tag mt
            join blade_reminder_group_tag t on t.id=mt.tag_id and t.group_id=mt.group_id where mt.group_id=? order by t.name
            """,groupId)) {
            String id=tag.remove("userId").toString();ids(tag);
            labels.computeIfAbsent(id,k->new ArrayList<>()).add(tag);
        }
        for(var row:rows) {
            ids(row);row.put("tags",labels.getOrDefault(row.get("id").toString(),List.of()));
            boolean self=user.toString().equals(row.get("id"));
            row.put("self",self);row.put("canRemind",self||permitted(row));
            row.put("permissionIssue",self||permitted(row)?"":friend(row)?"NO_PERMISSION":"NOT_FRIEND");
        }
        return rows;
    }

    public List<Map<String,Object>> tags(Long groupId) {
        var rows=jdbc.queryForList("""
            select t.id,t.name,(select count(*) from blade_reminder_group_member_tag mt
              join blade_reminder_group_member m on m.group_id=mt.group_id and m.user_id=mt.user_id and m.status='ACTIVE'
              join blade_user u on u.id=m.user_id and u.is_deleted=0
              where mt.group_id=t.group_id and mt.tag_id=t.id) as memberCount
            from blade_reminder_group_tag t where t.group_id=? order by t.name,t.id
            """,groupId);
        rows.forEach(this::ids);return rows;
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> create(Long user,ReminderGroupDtos.Save input) {
        if(!"CUSTOM".equals(input.getGroupType()))throw new ServiceException("沿用组织暂未开放，请选择自定义群组");
        String name=text(input.getName(),60,true),intro=text(input.getIntroduction(),500,false),avatar=avatar(input.getAvatar());
        Long id=IdWorker.getId();
        jdbc.update("insert into blade_reminder_group(id,owner_user_id,name,avatar,introduction) values(?,?,?,?,?)",id,user,name,avatar,intro);
        jdbc.update("insert into blade_reminder_group_member(group_id,user_id,member_role) values(?,?,'OWNER')",id,user);
        return detail(user,id);
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> save(Long user,ReminderGroupDtos.Save input) {
        access(user,input.getGroupId(),true,true);
        if(!"CUSTOM".equals(input.getGroupType()))throw new ServiceException("沿用组织暂未开放");
        jdbc.update("update blade_reminder_group set name=?,avatar=?,introduction=? where id=?",text(input.getName(),60,true),avatar(input.getAvatar()),text(input.getIntroduction(),500,false),input.getGroupId());
        touch(input.getGroupId());return detail(user,input.getGroupId());
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> invite(Long user,ReminderGroupDtos.Members input) {
        owner(access(user,input.getGroupId(),true,true));
        Set<Long> people=people(input.getUserIds());
        Integer count=jdbc.queryForObject("select count(*) from blade_reminder_group_member where group_id=? and status='ACTIVE'",Integer.class,input.getGroupId());
        if(count+people.size()>2000)throw new ServiceException("群组最多支持2000名成员");
        for(Long target:people) {
            if(target.equals(user))continue;
            if(jdbc.queryForObject("select count(*) from blade_friendship f join blade_user u on u.id=f.friend_user_id and u.is_deleted=0 where f.owner_user_id=? and f.friend_user_id=? and f.status='ACTIVE'",Integer.class,user,target)==0)
                throw new ServiceException("只能邀请自己已通过的好友，请刷新好友列表");
            var existing=jdbc.queryForList("select status from blade_reminder_group_member where group_id=? and user_id=?",input.getGroupId(),target);
            if(existing.isEmpty())jdbc.update("insert into blade_reminder_group_member(group_id,user_id) values(?,?)",input.getGroupId(),target);
            else if(!"ACTIVE".equals(existing.get(0).get("status")))jdbc.update("update blade_reminder_group_member set member_role='MEMBER',status='ACTIVE',joined_at=now() where group_id=? and user_id=?",input.getGroupId(),target);
        }
        touch(input.getGroupId());return detail(user,input.getGroupId());
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> role(Long user,ReminderGroupDtos.Member input) {
        owner(access(user,input.getGroupId(),true,true));
        Map<String,Object> member=member(input.getGroupId(),input.getUserId());
        if("OWNER".equals(member.get("member_role")))throw new ServiceException("不能修改群主角色");
        if(!List.of("ADMIN","MEMBER").contains(Objects.toString(input.getRole(),"")))throw new ServiceException("请选择管理员或群员");
        jdbc.update("update blade_reminder_group_member set member_role=? where group_id=? and user_id=?",input.getRole(),input.getGroupId(),input.getUserId());
        touch(input.getGroupId());return detail(user,input.getGroupId());
    }

    /** Save the administrator selection together, preserving the owner's role. */
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> admins(Long user,ReminderGroupDtos.Members input) {
        owner(access(user,input.getGroupId(),true,true));
        Set<Long> selected=people(input.getUserIds());
        for(Long id:selected) {
            if("OWNER".equals(member(input.getGroupId(),id).get("member_role")))
                throw new ServiceException("群主身份固定，无需设为管理员");
        }
        jdbc.update("update blade_reminder_group_member set member_role='MEMBER' where group_id=? and status='ACTIVE' and member_role='ADMIN'",input.getGroupId());
        for(Long id:selected)jdbc.update("update blade_reminder_group_member set member_role='ADMIN' where group_id=? and user_id=? and status='ACTIVE'",input.getGroupId(),id);
        touch(input.getGroupId());return detail(user,input.getGroupId());
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> memberTags(Long user,ReminderGroupDtos.Member input) {
        var group=access(user,input.getGroupId(),false,true);
        if(!user.equals(input.getUserId())&&!manager(group.get("myRole")))throw new ServiceException("只能设置自己的标签");
        member(input.getGroupId(),input.getUserId());
        List<String> names=tagNames(input.getTags());
        jdbc.update("delete from blade_reminder_group_member_tag where group_id=? and user_id=?",input.getGroupId(),input.getUserId());
        for(String name:names) {
            Long tag=tag(input.getGroupId(),name);
            jdbc.update("insert into blade_reminder_group_member_tag(group_id,user_id,tag_id) values(?,?,?)",input.getGroupId(),input.getUserId(),tag);
        }
        touch(input.getGroupId());return detail(user,input.getGroupId());
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> saveTag(Long user,ReminderGroupDtos.Tag input) {
        access(user,input.getGroupId(),true,true);
        String name=text(input.getName(),40,true);Set<Long> selected=people(input.getUserIds());
        for(Long id:selected)member(input.getGroupId(),id);
        Long tagId=input.getTagId();
        if(tagId==null)tagId=tag(input.getGroupId(),name);
        else {
            if(jdbc.queryForObject("select count(*) from blade_reminder_group_tag where group_id=? and id=?",Integer.class,input.getGroupId(),tagId)==0)throw new ServiceException("标签不存在");
            if(jdbc.queryForObject("select count(*) from blade_reminder_group_tag where group_id=? and name=? and id<>?",Integer.class,input.getGroupId(),name,tagId)>0)throw new ServiceException("该群组已有同名标签");
            jdbc.update("update blade_reminder_group_tag set name=? where group_id=? and id=?",name,input.getGroupId(),tagId);
        }
        jdbc.update("delete from blade_reminder_group_member_tag where group_id=? and tag_id=?",input.getGroupId(),tagId);
        for(Long id:selected)jdbc.update("insert into blade_reminder_group_member_tag(group_id,user_id,tag_id) values(?,?,?)",input.getGroupId(),id,tagId);
        touch(input.getGroupId());return detail(user,input.getGroupId());
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> deleteTag(Long user,ReminderGroupDtos.Tag input) {
        access(user,input.getGroupId(),true,true);
        jdbc.update("delete from blade_reminder_group_member_tag where group_id=? and tag_id=?",input.getGroupId(),input.getTagId());
        jdbc.update("delete from blade_reminder_group_tag where group_id=? and id=?",input.getGroupId(),input.getTagId());
        touch(input.getGroupId());return detail(user,input.getGroupId());
    }

    @Transactional(rollbackFor=Exception.class)
    public void remove(Long user,ReminderGroupDtos.Member input) {
        var group=access(user,input.getGroupId(),false,true);
        if(!user.equals(input.getUserId()))owner(group);
        if("OWNER".equals(member(input.getGroupId(),input.getUserId()).get("member_role")))throw new ServiceException("群主需解散群组，不能直接退出");
        jdbc.update("update blade_reminder_group_member set status='LEFT',member_role='MEMBER' where group_id=? and user_id=?",input.getGroupId(),input.getUserId());
        jdbc.update("delete from blade_reminder_group_member_tag where group_id=? and user_id=?",input.getGroupId(),input.getUserId());touch(input.getGroupId());
    }

    @Transactional(rollbackFor=Exception.class)
    public void dissolve(Long user,Long groupId) {
        owner(access(user,groupId,true,true));
        jdbc.update("update blade_reminder_group set status='DISSOLVED',revision=revision+1,update_time=now() where id=?",groupId);
    }

    /** Natural-language interpretation supplies exact IDs; this method expands and checks actual membership. */
    public List<Map<String,Object>> select(Long user,JsonNode selectors) {
        if(!selectors.isArray()||selectors.size()>30)throw new ServiceException("群组选择条件无效");
        LinkedHashMap<String,Map<String,Object>> result=new LinkedHashMap<>();
        for(JsonNode selector:selectors) {
            Long groupId=id(selector.path("groupId").asText());
            Map<String,Object> group=access(user,groupId,true,false);
            String mode=selector.path("tagMatch").asText("ALL");
            if(!List.of("ALL","ANY").contains(mode))throw new ServiceException("请明确标签需要同时满足还是满足其中一项");
            Set<String> labels=stringIds(selector.path("tagIds")),users=stringIds(selector.path("userIds")),excluded=stringIds(selector.path("excludedUserIds"));
            Set<String> available=new HashSet<>();for(var tag:tags(groupId))available.add(tag.get("id").toString());
            if(!available.containsAll(labels))throw new ServiceException("群组标签已变化，请重新选择标签");
            List<Map<String,Object>> members=members(user,groupId);
            Set<String> memberIds=new HashSet<>();members.forEach(m->memberIds.add(m.get("id").toString()));
            if(!memberIds.containsAll(users))throw new ServiceException("选择的人员已不在该群组，请重新确认");
            int matches=0;
            for(var person:members) {
                String personId=person.get("id").toString();
                @SuppressWarnings("unchecked") var personalTags=(List<Map<String,Object>>)person.get("tags");
                Set<String> tagIds=new HashSet<>();personalTags.forEach(t->tagIds.add(t.get("id").toString()));
                boolean tagMatch=labels.isEmpty()||("ANY".equals(mode)?labels.stream().anyMatch(tagIds::contains):tagIds.containsAll(labels));
                if(!tagMatch||!users.isEmpty()&&!users.contains(personId)||excluded.contains(personId))continue;
                matches++;
                Map<String,Object> merged=result.computeIfAbsent(personId,k->{var copy=new LinkedHashMap<>(person);copy.put("groupSources",new ArrayList<Map<String,Object>>());return copy;});
                @SuppressWarnings("unchecked") var sources=(List<Map<String,Object>>)merged.get("groupSources");
                if(sources.stream().noneMatch(s->groupId.toString().equals(s.get("groupId"))))sources.add(Map.of("groupId",groupId.toString(),"groupName",group.get("name"),"tags",personalTags));
            }
            if(matches==0)throw new ServiceException("群组“"+group.get("name")+"”没有匹配的成员，请调整人员或标签条件");
        }
        return new ArrayList<>(result.values());
    }

    /** On-demand model lookup: no people from unauthorized groups, no guessed identity binding. */
    public List<Map<String,Object>> lookup(Long user,JsonNode queries) {
        if(!queries.isArray()||queries.isEmpty()||queries.size()>10)throw new ServiceException("请明确需要查询的群组");
        List<Map<String,Object>> result=new ArrayList<>();
        for(JsonNode query:queries) {
            Long groupId=id(query.path("groupId").asText());var group=access(user,groupId,true,false);
            // Return complete relevant roster to let the model distinguish ambiguous names/tag meanings.
            List<Map<String,Object>> roster=select(user,mapper.createArrayNode().add(query));
            List<Map<String,Object>> compact=new ArrayList<>();
            for(var person:roster) {
                var entry=new LinkedHashMap<String,Object>();
                for(String key:List.of("id","account","name","realName","friendRemark","memberRole","tags","canRemind","permissionIssue"))entry.put(key,person.get(key));
                compact.add(entry);
            }
            result.add(Map.of("groupId",groupId.toString(),"groupName",group.get("name"),"members",compact));
        }
        return result;
    }

    public void validateSavedRecipients(Long user,JsonNode event) {
        for(JsonNode selector:event.path("groupSelectors"))access(user,id(selector.path("groupId").asText()),true,true);
        for(JsonNode person:event.path("recipients")) {
            Long target=id(person.path("id").asText());
            if(!target.equals(user)&&!canRemind(user,target))throw new ServiceException("“"+person.path("name").asText(person.path("account").asText())+"”的好友关系或提醒权限已变化，请查看人员并重新确认");
            JsonNode sources=person.path("groupSources");
            if(sources.isArray()&&!sources.isEmpty()) {
                boolean active=false;
                for(JsonNode source:sources) {
                    Long group=id(source.path("groupId").asText());access(user,group,true,false);
                    if(jdbc.queryForObject("select count(*) from blade_reminder_group_member where group_id=? and user_id=? and status='ACTIVE'",Integer.class,group,target)>0)active=true;
                }
                if(!active)throw new ServiceException("接收人的群组成员关系已变化，请查看人员并重新确认");
            }
        }
    }

    public boolean canRemind(Long user,Long target) {
        return user.equals(target)||jdbc.queryForObject("select count(*) from blade_friendship f join blade_user u on u.id=f.friend_user_id and u.is_deleted=0 where f.owner_user_id=? and f.friend_user_id=? and f.status='ACTIVE' and f.permission_mode in ('MUTUAL','I_CAN_REMIND')",Integer.class,user,target)>0;
    }

    public Map<String,Object> access(Long user,Long groupId,boolean management,boolean lock) {
        if(groupId==null)throw new ServiceException("群组不存在");
        var rows=jdbc.queryForList("select * from blade_reminder_group where id=? and status='ACTIVE'"+(lock?" for update":""),groupId);
        if(rows.isEmpty())throw new ServiceException("群组已解散或不存在");
        var memberships=jdbc.queryForList("select member_role from blade_reminder_group_member where group_id=? and user_id=? and status='ACTIVE'",groupId,user);
        if(memberships.isEmpty())throw new ServiceException("你已不在该群组中");
        String role=memberships.get(0).get("member_role").toString();
        if(management&&!manager(role))throw new ServiceException("仅群主和管理员可以管理群组或以群组发起提醒");
        var row=new LinkedHashMap<>(rows.get(0));
        row.put("name",row.get("name"));row.put("myRole",role);row.put("ownerUserId",row.remove("owner_user_id"));row.put("groupType",row.remove("group_type"));
        return row;
    }

    private Map<String,Object> member(Long group,Long user) {
        var rows=jdbc.queryForList("select m.* from blade_reminder_group_member m join blade_user u on u.id=m.user_id and u.is_deleted=0 where m.group_id=? and m.user_id=? and m.status='ACTIVE'",group,user);
        if(rows.isEmpty())throw new ServiceException("该人员已不在群组中");return rows.get(0);
    }
    private Long tag(Long group,String name) {
        var rows=jdbc.queryForList("select id from blade_reminder_group_tag where group_id=? and name=?",group,name);
        if(!rows.isEmpty())return ((Number)rows.get(0).get("id")).longValue();
        if(jdbc.queryForObject("select count(*) from blade_reminder_group_tag where group_id=?",Integer.class,group)>=200)throw new ServiceException("群组最多支持200个标签");
        Long id=IdWorker.getId();jdbc.update("insert into blade_reminder_group_tag(id,group_id,name) values(?,?,?)",id,group,name);return id;
    }
    private void touch(Long group) {jdbc.update("update blade_reminder_group set revision=revision+1,update_time=now() where id=?",group);}
    private void owner(Map<String,Object> group) {if(!"OWNER".equals(group.get("myRole")))throw new ServiceException("此操作仅群主可执行");}
    private boolean manager(Object role) {return "OWNER".equals(role)||"ADMIN".equals(role);}
    private boolean friend(Map<String,Object> person) {return person.get("isFriend") instanceof Number n&&n.intValue()==1;}
    private boolean permitted(Map<String,Object> person) {return friend(person)&&List.of("MUTUAL","I_CAN_REMIND").contains(Objects.toString(person.get("permissionMode"),""));}
    private void ids(Map<String,Object> row) {for(String key:List.of("id","ownerUserId","userId","tagId","groupId"))if(row.get(key)!=null)row.put(key,row.get(key).toString());}
    public static Long id(String value) {try{long id=Long.parseLong(value);if(id<=0)throw new NumberFormatException();return id;}catch(Exception ex){throw new ServiceException("人员、群组或标签标识无效，请重新选择");}}
    private Set<String> stringIds(JsonNode values) {Set<String> ids=new LinkedHashSet<>();if(!values.isMissingNode()&&!values.isNull()&&!values.isArray())throw new ServiceException("人员或标签条件格式无效，请重新确认");if(values.size()>2000)throw new ServiceException("人员或标签选择过多");if(values.isArray())for(JsonNode value:values)ids.add(id(value.asText()).toString());return ids;}
    private Set<Long> people(List<Long> values) {if(values==null||values.size()>2000||values.stream().anyMatch(v->v==null||v<=0))throw new ServiceException("人员选择无效");return new LinkedHashSet<>(values);}
    private List<String> tagNames(List<String> names) {if(names==null||names.size()>30)throw new ServiceException("每人最多设置30个标签");var result=new LinkedHashMap<String,String>();for(String name:names){String clean=text(name,40,true);result.putIfAbsent(clean.toLowerCase(Locale.ROOT),clean);}return new ArrayList<>(result.values());}
    private String text(String value,int max,boolean required) {String clean=Normalizer.normalize(Objects.toString(value,"").trim(),Normalizer.Form.NFKC);if(required&&clean.isBlank()||clean.length()>max||clean.chars().anyMatch(c->c<32&&c!='\n'))throw new ServiceException("名称或介绍为空、过长或包含无效字符");return clean;}
    private String avatar(String value) {String url=Objects.toString(value,"").trim();if(url.isEmpty())return "/avatars/group/01-campus.png";if(url.length()>1000||url.contains("\\")||url.contains("\n")||!(url.startsWith("/avatars/")||url.startsWith("/uploads/")||url.matches("https?://[^\\s]+")))throw new ServiceException("头像地址无效，请重新上传图片");return url;}
}
