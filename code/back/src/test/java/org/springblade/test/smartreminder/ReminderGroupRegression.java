package org.springblade.test.smartreminder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springblade.modules.smartreminder.config.ReminderGroupSchema;
import org.springblade.modules.smartreminder.dto.ReminderGroupDtos;
import org.springblade.modules.smartreminder.service.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;

/** Isolated MySQL-mode database regression; no production database/model connection. */
public class ReminderGroupRegression {
    static int assertions;
    static final ObjectMapper json=new ObjectMapper();
    static JdbcTemplate db;
    static ReminderGroupService groups;
    public static void main(String[] args) throws Exception {
        var data=new DriverManagerDataSource("jdbc:h2:mem:reminder_groups;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1","sa","");
        db=new JdbcTemplate(data);
        db.execute("create table blade_user(id bigint primary key,account varchar(45),name varchar(40),real_name varchar(40),avatar varchar(1000),is_deleted int default 0)");
        db.execute("create table blade_friendship(owner_user_id bigint,friend_user_id bigint,friend_remark varchar(30),permission_mode varchar(30),status varchar(20),primary key(owner_user_id,friend_user_id))");
        new ReminderGroupSchema(db).migrate();new ReminderGroupSchema(db).migrate();
        for(long id=1;id<=6;id++)db.update("insert into blade_user(id,account,name) values(?,?,?)",id,"user"+id,"人员"+id);
        for(long id=2;id<=5;id++)db.update("insert into blade_friendship values(?,?,null,?,'ACTIVE')",1L,id,id==5?"THEY_CAN_REMIND":"MUTUAL");
        var factory=new ProxyFactory(new ReminderGroupService(db,json));factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(data),new AnnotationTransactionAttributeSource()));
        groups=(ReminderGroupService)factory.getProxy();
        long a=create("校园巡检组"),b=create("迎新专项组");
        check(groups.list(1L).size()==2,"owner sees created groups");
        ReminderGroupDtos.Save bad=new ReminderGroupDtos.Save();bad.setName("学校");bad.setGroupType("ORGANIZATION");denied(()->groups.create(1L,bad));
        var forbiddenInvite=new ReminderGroupDtos.Members();forbiddenInvite.setGroupId(a);forbiddenInvite.setUserIds(List.of(2L,5L));
        denied(()->groups.invite(1L,forbiddenInvite));
        check(groups.members(1L,a).size()==1,"no partial invitation when one friend lacks outgoing reminder permission");
        db.update("update blade_friendship set permission_mode='MUTUAL' where owner_user_id=1 and friend_user_id=5");
        invite(a,2L,3L,4L,5L);invite(b,2L,3L);
        db.update("update blade_friendship set permission_mode='THEY_CAN_REMIND' where owner_user_id=1 and friend_user_id=5");
        check(groups.list(2L).size()==2,"invited friend joins without approval");
        check(groups.directory(2L).isEmpty(),"ordinary member has no AI send directory");
        var role=new ReminderGroupDtos.Member();role.setGroupId(a);role.setUserId(2L);role.setRole("ADMIN");groups.role(1L,role);
        check(groups.directory(2L).isEmpty(),"admin does not receive a group reminder directory");
        var adminSelector=json.createArrayNode().add(json.createObjectNode().put("groupId",Long.toString(a)));
        denied(()->groups.select(2L,adminSelector));denied(()->groups.lookup(2L,adminSelector));
        var preview=groups.list(1L).stream().filter(g->Long.toString(a).equals(g.get("id"))).findFirst().orElseThrow();
        check(preview.get("memberPreviews") instanceof List<?> avatars&&avatars.size()==4,"list supplies capped member avatars with total count");
        check(!groups.directory(1L).toString().contains("memberPreviews"),"avatar previews do not bloat AI context");
        var admins=new ReminderGroupDtos.Members();admins.setGroupId(a);admins.setUserIds(List.of(3L,4L));
        denied(()->groups.admins(2L,admins));groups.admins(1L,admins);
        check("MEMBER".equals(member(1L,a,2L).get("memberRole"))&&"ADMIN".equals(member(1L,a,3L).get("memberRole")),"administrator selection saves additions and removals together");
        check("OWNER".equals(member(1L,a,1L).get("memberRole")),"administrator save preserves owner");
        admins.setUserIds(List.of(2L,6L));denied(()->groups.admins(1L,admins));
        check("ADMIN".equals(member(1L,a,3L).get("memberRole")),"invalid administrator selection does not partially change roles");
        admins.setUserIds(List.of(1L));denied(()->groups.admins(1L,admins));
        admins.setUserIds(List.of());groups.admins(1L,admins);
        check("MEMBER".equals(member(1L,a,3L).get("memberRole")),"all appointed administrators can be removed");
        admins.setUserIds(List.of(2L));groups.admins(1L,admins);
        var adminInvite=new ReminderGroupDtos.Members();adminInvite.setGroupId(a);adminInvite.setUserIds(List.of(6L));denied(()->groups.invite(2L,adminInvite));
        var invalidInvite=new ReminderGroupDtos.Members();invalidInvite.setGroupId(a);invalidInvite.setUserIds(List.of(2L,6L));denied(()->groups.invite(1L,invalidInvite));
        check(groups.members(1L,a).size()==5,"nonfriend invitation fails transactionally");
        role.setUserId(3L);denied(()->groups.role(2L,role));
        var memberTags=new ReminderGroupDtos.Member();memberTags.setGroupId(a);memberTags.setUserId(3L);memberTags.setTags(List.of("消防负责人","东校区"));groups.memberTags(3L,memberTags);
        check(member(1L,a,3L).get("tags") instanceof List<?> labels&&labels.size()==2,"member can maintain multiple own tags");
        memberTags.setUserId(4L);denied(()->groups.memberTags(3L,memberTags));groups.memberTags(2L,memberTags);
        var tag=new ReminderGroupDtos.Tag();tag.setGroupId(a);tag.setName("材料负责人");tag.setUserIds(List.of(3L,4L));groups.saveTag(2L,tag);
        long material=tag(a,"材料负责人"),fire=tag(a,"消防负责人");
        var selector=json.createObjectNode().put("groupId",Long.toString(a));selector.putArray("tagIds").add(Long.toString(material));
        var selected=groups.select(1L,json.createArrayNode().add(selector));check(selected.size()==2,"tag expands to exact roster");
        selector.putArray("tagIds").add(Long.toString(material)).add(Long.toString(fire));selector.put("tagMatch","ALL");
        check(groups.select(1L,json.createArrayNode().add(selector)).size()==2,"tag intersection");
        memberTags.setUserId(4L);memberTags.setTags(List.of("材料负责人"));groups.memberTags(2L,memberTags);
        check(groups.select(1L,json.createArrayNode().add(selector)).size()==1,"latest tags, no stale membership cache");
        selector.put("tagMatch","ANY");check(groups.select(1L,json.createArrayNode().add(selector)).size()==2,"tag union");
        var both=json.createArrayNode().add(json.createObjectNode().put("groupId",Long.toString(a))).add(json.createObjectNode().put("groupId",Long.toString(b)));
        var union=groups.select(1L,both);check(union.size()==5,"cross-group recipients deduplicated");
        @SuppressWarnings("unchecked") var sources=(List<Map<String,Object>>)union.stream().filter(p->p.get("id").equals("3")).findFirst().orElseThrow().get("groupSources");
        check(sources.size()==2,"both provenance groups retained");
        check(!Boolean.TRUE.equals(member(2L,a,3L).get("canRemind")),"admin role never grants friendship permission");
        check(!Boolean.TRUE.equals(member(1L,a,5L).get("canRemind")),"one-way permission respected");
        denied(()->groups.select(3L,both));
        var lookup=groups.lookup(1L,json.createArrayNode().add(json.createObjectNode().put("groupId",Long.toString(a))));
        check(!lookup.toString().contains("phone")&&!lookup.toString().contains("email"),"model roster omits unrelated private fields");
        SmartEventContentService content=new SmartEventContentService(db,json,null);
        SmartReminderService service=new SmartReminderService(db,json,null,null,null,content,null,null,groups);
        var resolve=SmartReminderService.class.getDeclaredMethod("resolveGroupCandidate",Long.class,ObjectNode.class);resolve.setAccessible(true);
        ObjectNode event=json.createObjectNode().put("summary","明天下午提交预算").put("groupTask","明天下午提交预算");event.putArray("recipientNames");event.set("groupSelectors",both);
        var candidate=(ObjectNode)resolve.invoke(service,1L,event);
        check(candidate.path("recipients").size()==4&&candidate.path("recipientIssues").size()==1,"blocked member remains visible, not silently sent");
        check(candidate.path("recipientPool").size()==5,"complete matched roster retained");
        check(candidate.path("recipients").get(0).path("taskContent").asText().equals("明天下午提交预算"),"common group task avoids one model call per person");
        var invalid=json.createObjectNode().put("groupId",Long.toString(a)).put("tagIds","not-an-array");denied(()->groups.select(1L,json.createArrayNode().add(invalid)));
        var crossTag=json.createObjectNode().put("groupId",Long.toString(b));crossTag.putArray("tagIds").add(Long.toString(fire));denied(()->groups.select(1L,json.createArrayNode().add(crossTag)));
        ObjectNode manual=json.createObjectNode().put("summary","提交材料").put("groupTask","提交材料").put("manualRecipients",true);
        manual.set("groupSelectors",json.createArrayNode().add(selector));manual.putArray("recipientNames");manual.putArray("selectedRecipientIds").add("2");
        var adjusted=(ObjectNode)resolve.invoke(service,1L,manual);check(adjusted.path("recipients").size()==1&&"2".equals(adjusted.path("recipients").get(0).path("id").asText()),"manual review can add another member from authorized source groups");
        verifyModelLookup(a,fire,content);
        verifyLegacyCompatibility(content);
        groups.validateSavedRecipients(1L,candidate);
        denied(()->groups.validateSavedRecipients(2L,candidate));
        db.update("update blade_friendship set permission_mode='THEY_CAN_REMIND' where owner_user_id=1 and friend_user_id=3");denied(()->groups.validateSavedRecipients(1L,candidate));
        db.update("update blade_friendship set permission_mode='MUTUAL' where owner_user_id=1 and friend_user_id=3");
        var remove=new ReminderGroupDtos.Member();remove.setGroupId(a);remove.setUserId(4L);groups.remove(1L,remove);denied(()->groups.validateSavedRecipients(1L,candidate));
        var ownerTag=new ReminderGroupDtos.Member();ownerTag.setGroupId(a);ownerTag.setUserId(3L);ownerTag.setTags(List.of("管理员"));groups.memberTags(3L,ownerTag);
        check(!groups.directory(3L).iterator().hasNext(),"tag text cannot elevate role");
        denied(()->groups.dissolve(2L,a));groups.dissolve(1L,a);check(groups.list(2L).size()==1,"dissolved group disappears from member list");denied(()->groups.detail(2L,a));denied(()->groups.lookup(1L,json.createArrayNode().add(selector)));
        check(groups.directory(1L).size()==1,"dissolved group removed from AI context");
        content.closeSummaryWorker();
        System.out.println("PASS ReminderGroupRegression: "+assertions+" assertions; real isolated DB, transactional permissions, tags, multi-group recipients and revalidation");
    }
    static long create(String name){var input=new ReminderGroupDtos.Save();input.setName(name);return Long.parseLong(groups.create(1L,input).get("id").toString());}
    static void invite(long group,Long...people){var input=new ReminderGroupDtos.Members();input.setGroupId(group);input.setUserIds(List.of(people));groups.invite(1L,input);}
    static long tag(long group,String name){return groups.tags(group).stream().filter(t->name.equals(t.get("name"))).map(t->Long.parseLong(t.get("id").toString())).findFirst().orElseThrow();}
    static Map<String,Object> member(long user,long group,long person){return groups.members(user,group).stream().filter(p->Long.toString(person).equals(p.get("id"))).findFirst().orElseThrow();}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);assertions++;}
    static void denied(Runnable action){try{action.run();throw new AssertionError("Expected permission/validation failure");}catch(org.springblade.core.log.exception.ServiceException expected){assertions++;}}
    static void verifyModelLookup(long group,long fire,SmartEventContentService content) throws Exception {
        var lookup=json.createObjectNode().put("intent","lookup_group");
        var query=lookup.putArray("groupQueries").addObject().put("groupId",Long.toString(group));query.putArray("tagIds").add(Long.toString(fire));
        var ai=new ScriptedAi(List.of(lookup.toString(),"{\"intent\":\"create_event\",\"reply\":\"请核对人员\",\"events\":[]}"));
        var configService=new AiConfigService(null,null){@Override public String languageInstruction(Long user){return "";}};
        var service=new SmartReminderService(db,json,ai,configService,null,content,null,null,groups);
        var preparedType=Arrays.stream(SmartReminderService.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("PreparedChat")).findFirst().orElseThrow();
        var constructor=preparedType.getDeclaredConstructors()[0];constructor.setAccessible(true);
        var prepared=constructor.newInstance(1L,11L,"提醒巡检组管消防的人",null,null,"可发起提醒的群组目录："+json.writeValueAsString(groups.directory(1L)),List.of());
        var method=SmartReminderService.class.getDeclaredMethod("groupAwareAnswer",preparedType,AiConfigService.AiRuntimeConfig.class,java.util.function.Consumer.class);method.setAccessible(true);
        var config=new AiConfigService.AiRuntimeConfig(1L,"test","unused","unused","test",1_000_000,900_000,4096,new java.math.BigDecimal("0.3"),"",false,1000,null,null,true);
        var answer=(NewApiClient.AiAnswer)method.invoke(service,prepared,config,null);
        check(ai.prompts.size()==2&&answer.content().contains("create_event"),"group lookup stays in the same user turn and returns the final answer");
        check(ai.prompts.get(1).contains("人员3")&&ai.prompts.get(1).contains("消防负责人")&&!ai.prompts.get(1).contains("人员5"),"model receives the real filtered roster, not all unrelated group members");
        var repeated=new ScriptedAi(List.of(lookup.toString(),lookup.toString(),lookup.toString()));
        var bounded=new SmartReminderService(db,json,repeated,configService,null,content,null,null,groups);
        var clarification=(NewApiClient.AiAnswer)method.invoke(bounded,prepared,config,null);
        check(repeated.prompts.size()==3&&clarification.content().contains("clarify"),"repeated model lookups are bounded and finish with a clarification");
    }
    static void verifyLegacyCompatibility(SmartEventContentService content) throws Exception {
        var old=json.readValue("{\"content\":\"明天提醒我提交材料\",\"fileIds\":[]}",org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest.class);
        check(!Boolean.TRUE.equals(old.getSupportsGroupRecipients()),"old APP request without capability remains legacy");
        var nullFlag=json.readValue("{\"supportsGroupRecipients\":null}",org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest.class);
        check(!Boolean.TRUE.equals(nullFlag.getSupportsGroupRecipients()),"null capability is not an opt-in");
        var upgraded=json.readValue("{\"supportsGroupRecipients\":true}",org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest.class);
        check(Boolean.TRUE.equals(upgraded.getSupportsGroupRecipients()),"new client explicitly opts in");
        var oldConfirm=json.readValue("{\"candidateId\":5001}",org.springblade.modules.smartreminder.dto.SmartReminderDtos.CandidateConfirmRequest.class);
        check(!oldConfirm.getAcceptConflicts()&&!oldConfirm.getAcceptPartialRecipients(),"old confirmation flags preserve safe defaults");
        var configService=new AiConfigService(null,null){@Override public String languageInstruction(Long user){return "";}};
        var config=new AiConfigService.AiRuntimeConfig(1L,"test","unused","unused","test",1_000_000,900_000,4096,new java.math.BigDecimal("0.3"),"",false,1000,null,null,true);
        var preparedType=Arrays.stream(SmartReminderService.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("PreparedChat")).findFirst().orElseThrow();
        var constructor=preparedType.getDeclaredConstructors()[0];constructor.setAccessible(true);
        var prepared=constructor.newInstance(1L,11L,"提醒巡检组",null,null,"好友上下文",List.of());
        var method=SmartReminderService.class.getDeclaredMethod("groupAwareAnswer",preparedType,AiConfigService.AiRuntimeConfig.class,java.util.function.Consumer.class,boolean.class);method.setAccessible(true);
        for(String output:List.of("{\"intent\":\"lookup_group\",\"groupQueries\":[]}","{\"intent\":\"create_event\",\"events\":[{\"groupSelectors\":[{\"groupId\":\"1\"}]}]}")) {
            var ai=new ScriptedAi(List.of(output));
            // No group service at all: legacy requests must never query it, even if a model asks.
            var service=new SmartReminderService(db,json,ai,configService,null,content,null,null,null);
            var result=(NewApiClient.AiAnswer)method.invoke(service,prepared,config,null,false);
            check(result.content().contains("clarify")&&result.content().contains("升级APP")&&ai.prompts.size()==1,"legacy group output becomes readable upgrade guidance without querying members");
            check(!ai.systems.get(0).contains(ReminderGroupPrompts.RULES)&&ai.systems.get(0).contains("客户端兼容协议"),"old client does not receive new group protocol");
        }
        String plain="{\"intent\":\"create_event\",\"events\":[{\"recipientNames\":[\"我\"]}]}";
        var ai=new ScriptedAi(List.of(plain));
        var service=new SmartReminderService(db,json,ai,configService,null,content,null,null,null);
        check(((NewApiClient.AiAnswer)method.invoke(service,prepared,config,null,false)).content().equals(plain),"legacy self/friend event output remains unchanged");
    }
    static class ScriptedAi extends NewApiClient {
        final List<String> answers;final List<String> prompts=new ArrayList<>();final List<String> systems=new ArrayList<>();
        ScriptedAi(List<String> answers){super(null,json);this.answers=answers;}
        @Override public AiAnswer chat(AiConfigService.AiRuntimeConfig config,String system,String prompt,List<Map<String,Object>> images){prompts.add(prompt);systems.add(system);return new AiAnswer(answers.get(prompts.size()-1),"","test","");}
    }
}
