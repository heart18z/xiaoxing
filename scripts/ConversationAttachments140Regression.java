import java.util.*;
import org.springblade.core.log.exception.ServiceException;

/** Uses the real event-conversation selector and its existing privacy fixture. */
public class ConversationAttachments140Regression extends EventPrivacyRegression {
    static void forbidden(Runnable task,String label) {
        try {task.run();throw new AssertionError(label);}catch(ServiceException expected){check(true,label);}
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception {
        EventPrivacyRegression.main(args);
        String payload="{\"fileIds\":[\"501\",\"502\"],\"fileNames\":[\"boxes.jpg\",\"other.png\"],\"privateField\":\"DO-NOT-EXPOSE\"}";
        db.update("update blade_smart_chat_message set payload_json=? where id=181",payload);
        var creation=service.eventConversation(81L,-1L);
        var rows=(List<Map<String,Object>>)creation.get("messages");
        var source=rows.stream().filter(m->"181".equals(m.get("id").toString())).findFirst().orElseThrow();
        check(source.get("fileIds").equals(List.of("501","502")),"legacy creation source retains both image references");
        check(source.get("fileNames").equals(List.of("boxes.jpg","other.png")),"attachment filenames remain aligned with file IDs");
        check(!creation.toString().contains("DO-NOT-EXPOSE")&&!source.containsKey("payloadJson"),"attachment projection does not expose private message payload");
        check(service.conversationAttachmentOwner(81L,-1L,181L,501L)==-1L,"own historical attachment is authorized");
        forbidden(()->service.conversationAttachmentOwner(81L,-1L,181L,999L),"unrelated file ID cannot use a valid conversation scope");
        forbidden(()->service.conversationAttachmentOwner(82L,2L,181L,501L),"unrelated event cannot access historical attachment");
        db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,payload_json,event_id) values(301,2,'user','TEXT','已放好箱子',?,81)",payload);
        db.update("insert into blade_smart_message_event(event_id,user_id,message_id) values(81,2,301)");
        check(service.conversationAttachmentOwner(81L,2L,301L,501L)==2L,"creator can view a recipient image only through the authorized recipient conversation");
        forbidden(()->service.conversationAttachmentOwner(81L,-1L,301L,501L),"switching participant cannot grant access to another participant message");
        db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,payload_json) values(302,2,'user','TEXT','无关私聊图片',?)",payload);
        forbidden(()->service.conversationAttachmentOwner(81L,2L,302L,501L),"unlinked private message is inaccessible even with the same participant");
        content.closeSummaryWorker();
        System.out.println("PASS ConversationAttachments140Regression");
    }
}
