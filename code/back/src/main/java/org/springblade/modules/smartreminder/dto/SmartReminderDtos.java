package org.springblade.modules.smartreminder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class SmartReminderDtos {

	private SmartReminderDtos() {
	}

	@Data
	public static class AiConfigRequest {
		private String configType = "LLM";
		private Long id;
		private String configName;
		private String modelAlias;
		private Boolean systemDefault;
		private String baseUrl;
		private String apiKey;
		private String modelName;
		private Integer contextWindow = 1048576;
		private Integer maxInputTokens = 991000;
		private Integer maxTokens = 131072;
		private BigDecimal temperature = new BigDecimal("0.30");
		private String reasoningEffort = "xhigh";
		private String extraBody = "{}";
		private Boolean showThinking = true;
		private Integer requestTimeout = 120000;
		private String intentPrompt;
		private String decisionPrompt;
		private Boolean enabled = true;
	}

	@Data
	public static class ChatRequest {
		private String requestId;
		private String content;
		private List<Long> fileIds = new ArrayList<>();
	}

	@Data
	public static class ModelPreferenceRequest {
		private Long llmConfigId;
		private Long speechConfigId;
		private String llmMode;
		private String speechMode;
		private String language;
		private AiConfigRequest llm;
		private AiConfigRequest speech;
	}

	@Data
	public static class CandidateConfirmRequest {
		private Long candidateId;
		private Boolean acceptConflicts = false;
	}

	@Data
	public static class ProfileUpdateRequest {
		private String nickname;
		private String name;
		private String phone;
		private String email;
		private String avatar;
		private String aiAvatar;
	}

	@Data
	public static class FriendRequest {
		private Long targetUserId;
		private String message;
		private String permissionMode = "MUTUAL";
	}

	@Data
	public static class FriendReplyRequest {
		private Long requestId;
		private Boolean accept;
	}

	@Data
	public static class FriendRemarkRequest {
		private Long targetUserId;
		private String remark;
	}

	@Data
	public static class StopRequest {
		private Long eventId;
		private Long branchId;
		private String reason;
	}

	@Data
	public static class ReadRequest {
		private List<Long> messageIds = new ArrayList<>();
	}
}
