package org.springblade.modules.smartreminder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.CandidateConfirmRequest;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.FriendReplyRequest;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.FriendRequest;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.ProfileUpdateRequest;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.ReadRequest;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.StopRequest;
import org.springblade.modules.smartreminder.service.SmartFileService;
import org.springblade.modules.smartreminder.service.SmartReminderService;
import org.springblade.modules.smartreminder.service.SmartSocialService;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
@RequestMapping("app/reminder")
public class SmartReminderController {

	private final SmartReminderService reminderService;
	private final SmartFileService fileService;
	private final ObjectMapper objectMapper;
	private final SmartSocialService socialService;
	private final org.springblade.modules.smartreminder.service.AiConfigService aiConfigService;
	private final org.springblade.modules.smartreminder.service.SpeechService speechService;
	private final org.springblade.modules.smartreminder.service.ChatRunRegistry chatRuns;
	private final org.springblade.modules.smartreminder.service.ChatJobService chatJobs;
	private final org.springblade.modules.smartreminder.service.ReminderDrawerService drawer;

	@PostMapping("/chat/jobs/submit")
	public R<Object> submitJob(@RequestBody ChatRequest request){AppRoleGuard.requireAppUser();return R.data(chatJobs.submit(AuthUtil.getUserId(),AuthUtil.getUserAccount(),request));}
	@PostMapping("/chat/jobs/status")
	public R<Object> jobStatus(@RequestBody ChatRequest request){AppRoleGuard.requireAppUser();return R.data(chatJobs.status(AuthUtil.getUserId(),request.getRequestId()));}
	@PostMapping(value="/chat/jobs/watch", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<StreamingResponseBody> watchJob(@RequestBody ChatRequest request){
		AppRoleGuard.requireAppUser();
		Long user=AuthUtil.getUserId();String id=request.getRequestId();
		chatJobs.status(user,id);
		StreamingResponseBody body=output->{
			try{writeSse(output,Map.of("type","ready"));chatJobs.observe(user,id,event->writeUnchecked(output,event));}
			catch(UncheckedIOException disconnected){/* Observer disconnected; accepted work continues. */}
			catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
		};
		return ResponseEntity.ok().cacheControl(CacheControl.noCache()).header("X-Accel-Buffering","no").contentType(MediaType.TEXT_EVENT_STREAM).body(body);
	}
	@PostMapping("/events/drawer")
	public R<Object> drawer(@RequestParam(defaultValue="sent") String type){AppRoleGuard.requireAppUser();return R.data(drawer.list(AuthUtil.getUserId(),"received".equals(type)));}

	@PostMapping("/chat/stop")
	public R<Object> stopChat(@RequestBody ChatRequest request){AppRoleGuard.requireAppUser();return R.data(Map.of("stopped",chatRuns.stop(AuthUtil.getUserId(),request.getRequestId())));}

	@PostMapping("/chat/context/clear")
	public R<Object> clearChatContext() {
		AppRoleGuard.requireAppUser();
		if(chatRuns.hasActive(AuthUtil.getUserId()))throw new org.springblade.core.log.exception.ServiceException("请先停止生成或等待本轮结束，再清除上下文");
		return R.data(reminderService.clearChatContext());
	}

	@PostMapping("/settings/models")
	public R<Object> modelSettings() { AppRoleGuard.requireAppUser();return R.data(aiConfigService.preferences(AuthUtil.getUserId())); }

	@PostMapping("/settings/models/save")
	public R<Object> saveModelSettings(@RequestBody org.springblade.modules.smartreminder.dto.SmartReminderDtos.ModelPreferenceRequest request) {
		AppRoleGuard.requireAppUser();return R.data(aiConfigService.savePreferences(AuthUtil.getUserId(),request));
	}

	@PostMapping("/audio/transcriptions")
	public R<Object> transcribe(@RequestParam("file") MultipartFile file) {
		AppRoleGuard.requireAppUser();return R.data(speechService.transcribe(AuthUtil.getUserId(),file));
	}

	@PostMapping("/friends/remove")
	public R<Object> removeFriend(@RequestBody FriendRequest request) {
		AppRoleGuard.requireAppUser();
		socialService.removeFriend(AuthUtil.getUserId(),request.getTargetUserId());
		return R.success("好友关系已解除，历史事件与消息仍保留");
	}

	@PostMapping("/friends/remark")
	public R<Object> saveFriendRemark(@RequestBody org.springblade.modules.smartreminder.dto.SmartReminderDtos.FriendRemarkRequest request) {
		AppRoleGuard.requireAppUser();
		socialService.saveRemark(AuthUtil.getUserId(), request.getTargetUserId(), request.getRemark());
		return R.success("好友备注已保存");
	}

	@PostMapping("/bootstrap")
	public R<Object> bootstrap() {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.bootstrap());
	}

	@PostMapping("/profile/update")
	public R<Object> updateProfile(@RequestBody ProfileUpdateRequest request) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.updateProfile(request));
	}

	@PostMapping("/chat/messages")
	public R<Object> messages(@RequestParam(defaultValue = "100") int limit) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.chatMessages(limit));
	}

	@PostMapping("/chat/sync")
	public R<Object> syncMessages(@RequestParam(defaultValue = "150") int limit,
		@RequestParam(defaultValue = "") String revision) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.syncChatMessages(limit, revision));
	}

	@PostMapping("/chat/send")
	public R<Object> send(@RequestBody ChatRequest request) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.sendChat(request));
	}

	@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<StreamingResponseBody> stream(@RequestBody ChatRequest request) {
		AppRoleGuard.requireAppUser();
		Long userId = AuthUtil.getUserId();
		String account = AuthUtil.getUserAccount();
		if(request.getRequestId()==null)request.setRequestId(java.util.UUID.randomUUID().toString());
		var run=chatRuns.register(userId,request.getRequestId());
		StreamingResponseBody body = output -> {
			long started=System.nanoTime();
			String trace=java.util.UUID.randomUUID().toString().substring(0,8);
			try {
				chatRuns.attach(run);
				// Flush an SSE frame immediately so the client knows the stream is connected
				// while the upstream model is still preparing its first token.
				writeSse(output, Map.of("type", "ready"));
				Map<String, Object> result = reminderService.sendChatStream(request, userId, account,
					delta -> writeUnchecked(output, Map.of("type", "delta", "content", delta)),
					delta -> writeUnchecked(output, Map.of("type", "reasoning", "content", delta)));
				// sendChatStream has returned through its transactional proxy: commit succeeded.
				writeSse(output, Map.of("type", "delta", "content", java.util.Objects.toString(result.get("reply"), "")));
				writeSse(output, Map.of("type", "result", "data", result));
			} catch (Exception e) {
				// No prompts, credentials, SQL parameters or raw exception messages in logs/UI.
				log.warn("chat_stream_failed trace={} user={} elapsedMs={} errorType={}",trace,userId,(System.nanoTime()-started)/1_000_000,e.getClass().getSimpleName());
				writeSse(output, Map.of("type", "error", "message", safeMessage(e)+"（编号："+trace+"）"));
			} finally {chatRuns.finish(run);}
		};
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noCache())
			.header("X-Accel-Buffering", "no")
			.contentType(MediaType.TEXT_EVENT_STREAM)
			.body(body);
	}

	private void writeUnchecked(OutputStream output, Map<String, Object> event) {
		try {
			writeSse(output, event);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void writeSse(OutputStream output, Map<String, Object> event) throws IOException {
		String value = "data:" + objectMapper.writeValueAsString(event) + "\n\n";
		output.write(value.getBytes(StandardCharsets.UTF_8));
		output.flush();
	}

	private String safeMessage(Exception exception) {
		Throwable cause = exception instanceof UncheckedIOException && exception.getCause() != null
			? exception.getCause() : exception;
		if(cause instanceof org.springblade.core.log.exception.ServiceException) {
			String message=cause.getMessage();
			if(message!=null&&!message.isBlank()&&message.length()<=160&&!message.matches("(?is).*\\b(sql|jdbc|java\\.)\\b.*"))return message;
		}
		return "本次操作未完成，请检查事件状态后重试";
	}

	@PostMapping("/chat/read")
	public R<Object> read(@RequestBody ReadRequest request) {
		AppRoleGuard.requireAppUser();
		reminderService.markRead(request.getMessageIds());
		return R.success("已读");
	}

	@PostMapping("/candidate/confirm")
	public R<Object> confirm(@RequestBody CandidateConfirmRequest request) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.confirmWithScheduleReview(request.getCandidateId(),Boolean.TRUE.equals(request.getAcceptConflicts())));
	}

	@PostMapping("/files/upload")
	public R<Object> upload(@RequestParam("file") MultipartFile file) {
		AppRoleGuard.requireAppUser();
		return R.data(fileService.upload(file));
	}

	@PostMapping("/files/preview")
	public R<Object> filePreview(@RequestParam Long id, @RequestParam(defaultValue="false") boolean full) {
		AppRoleGuard.requireAppUser();
		return R.data(fileService.preview(id, AuthUtil.getUserId(),full));
	}

    /** QR codes contain only a public account identifier, never a login credential. */
    @PostMapping("/friends/qr")
    public R<Object> friendQr() {
        AppRoleGuard.requireAppUser();
        String payload = "xiaoxing:friend:v1:" + AuthUtil.getUserAccount();
        byte[] png = cn.hutool.extra.qrcode.QrCodeUtil.generatePng(payload, 600, 600);
        return R.data(Map.of("image", "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png)));
    }

	@PostMapping("/friends/search")
	public R<Object> searchUsers(@RequestParam String keyword) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.searchUsers(keyword));
	}

	@PostMapping("/friends/list")
	public R<Object> friends() {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.friendList());
	}

	@PostMapping("/friends/requests")
	public R<Object> friendRequests() {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.friendRequests());
	}

	@PostMapping("/friends/request")
	public R<Object> requestFriend(@RequestBody FriendRequest request) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.sendFriendRequest(request.getTargetUserId(), request.getMessage(), request.getPermissionMode()));
	}

	@PostMapping("/friends/reply")
	public R<Object> replyFriend(@RequestBody FriendReplyRequest request) {
		AppRoleGuard.requireAppUser();
		reminderService.replyFriendRequest(request.getRequestId(), Boolean.TRUE.equals(request.getAccept()));
		return R.success("已处理");
	}

	@PostMapping("/events")
	public R<Object> events(@RequestParam(defaultValue = "sent") String type,
							@RequestParam(defaultValue = "") String status) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.listEvents(type, status));
	}

	@PostMapping("/events/counts")
	public R<Object> eventCounts(@RequestParam(defaultValue = "sent") String type) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.eventCounts(type));
	}

	@PostMapping("/event/detail")
	public R<Object> detail(@RequestParam Long eventId) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.eventDetail(eventId));
	}

	@PostMapping("/event/conversation")
	public R<Object> conversation(@RequestParam Long eventId, @RequestParam Long participantUserId) {
		AppRoleGuard.requireAppUser();
		return R.data(reminderService.eventConversation(eventId, participantUserId));
	}

	@PostMapping("/event/stop")
	public R<Object> stop(@RequestBody StopRequest request) {
		AppRoleGuard.requireAppUser();
		reminderService.stop(request.getEventId(), request.getBranchId(), request.getReason());
		return R.success("已停止");
	}
}
