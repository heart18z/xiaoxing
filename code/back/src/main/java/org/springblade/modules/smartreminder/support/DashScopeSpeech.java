package org.springblade.modules.smartreminder.support;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** DashScope Flash ASR uses JSON audio input and its own output envelope. */
public final class DashScopeSpeech {
    public static final String ENDPOINT = "/api/v1/services/aigc/multimodal-generation/generation";
    private DashScopeSpeech() {}
    public static boolean matches(String url) { return url.endsWith(ENDPOINT); }
    public static Map<String,Object> body(String model, byte[] audio, String extension, Map<String,Object> extra) {
        // The data URL must fit the provider's 10 MB inline audio limit.
        if (audio.length > 7L * 1024 * 1024) throw new IllegalArgumentException("此语音渠道支持7MB以内的录音，请分段录制");
        String mime = switch (extension) {
            case "mp3" -> "audio/mpeg";
            case "m4a", "mp4" -> "audio/mp4";
            default -> "audio/" + extension;
        };
        Map<String,Object> parameters = new LinkedHashMap<>();
        if (extra.get("parameters") instanceof Map<?,?> configured)
            configured.forEach((key,value) -> parameters.put(key.toString(),value));
        // Encoded files carry their own sample rate; do not mislabel 44.1/48 kHz audio as 16 kHz.
        parameters.remove("sample_rate");
        parameters.put("format", extension);
        String data = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(audio);
        return Map.of("model", model, "input", Map.of("messages", List.of(Map.of("role", "user", "content",
            List.of(Map.of("type", "input_audio", "input_audio", Map.of("data", data)))))), "parameters", parameters);
    }
    public static String text(JsonNode root) {
        if (root.hasNonNull("code") && !root.path("code").asText().isBlank())
            throw new IllegalArgumentException("语音渠道返回错误，请检查模型与服务配置");
        JsonNode output = root.path("output");
        if (output.path("text").isTextual()) return output.path("text").asText().trim();
        return output.path("output").path("sentence").path("text").asText("").trim();
    }
}
