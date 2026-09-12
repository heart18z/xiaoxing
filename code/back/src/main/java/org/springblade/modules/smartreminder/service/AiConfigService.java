package org.springblade.modules.smartreminder.service;

import lombok.RequiredArgsConstructor;
import org.springblade.core.mp.support.SqlKeyword;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.AiConfigRequest;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springblade.modules.smartreminder.support.ModelBody;
import org.springblade.modules.smartreminder.support.PersonalModelEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiConfigService {

	private final JdbcTemplate jdbcTemplate;
	private final SecretCodec secretCodec;

	public Map<String, Object> currentMasked() {
		return currentMasked("LLM", null);
	}

	public Map<String, Object> currentMasked(String type, Long id) {
		type = configType(type);
		AiRuntimeConfig config = currentInternal(type, id, false);
		if (config == null) return new LinkedHashMap<>();
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", config.id());
		result.put("configType", type);
		result.put("configName", config.configName());
		result.put("baseUrl", config.baseUrl());
		result.put("apiKey", mask(config.apiKey()));
		result.put("modelName", config.modelName());
		result.put("contextWindow", config.contextWindow());
		result.put("maxInputTokens", config.maxInputTokens());
		result.put("maxTokens", config.maxTokens());
		result.put("temperature", config.temperature());
		result.put("reasoningEffort", config.reasoningEffort());
		result.put("extraBody", config.extraBody());
		result.put("showThinking", config.showThinking());
		result.put("requestTimeout", config.requestTimeout());
		result.put("intentPrompt", Func.isBlank(config.intentPrompt()) ? SmartReminderPrompts.INTENT : config.intentPrompt());
		result.put("decisionPrompt", Func.isBlank(config.decisionPrompt()) ? SmartReminderPrompts.DECISION : config.decisionPrompt());
		result.put("enabled", config.enabled());
		Map<String,Object> flags=jdbcTemplate.queryForMap("select model_alias,system_default from blade_ai_service_config where id=?",config.id());
		result.put("modelAlias",flags.get("model_alias"));
		result.put("systemDefault",((Number)flags.get("system_default")).intValue()==1);
		return result;
	}

	public AiRuntimeConfig enabledConfig() {
		return enabledConfigForUser(AuthUtil.getUserId(), "LLM");
	}
	public AiRuntimeConfig testLlmConfig(Long id) {
		AiRuntimeConfig config=currentInternal("LLM",id,false);
		if(config==null)throw new IllegalArgumentException("请先保存 LLM 配置");
		return config;
	}

	public AiRuntimeConfig enabledConfigForUser(Long userId, String type) {
		type = configType(type);
		if(userId!=null){
			String prefix="LLM".equals(type)?"llm":"speech";
			List<Map<String,Object>> personal=jdbcTemplate.queryForList("select "+prefix+"_mode as mode,"+prefix+"_personal as config from blade_smart_user_preference where user_id=?",userId);
			if(!personal.isEmpty()&&"PERSONAL".equals(personal.get(0).get("mode"))){
				AiConfigRequest request=decodePersonal((String)personal.get(0).get("config"));
				if(request==null)throw new IllegalStateException("个人模型配置不完整，请在设置中完善");
				PersonalModelEndpoint.validate(request.getBaseUrl());
				return new AiRuntimeConfig(-userId,"个人模型",request.getBaseUrl(),request.getApiKey(),request.getModelName(),defaultInt(request.getContextWindow(),1048576),defaultInt(request.getMaxInputTokens(),991000),defaultInt(request.getMaxTokens(),8192),defaultDecimal(request.getTemperature()),"",!Boolean.FALSE.equals(request.getShowThinking()),Math.min(300000,defaultInt(request.getRequestTimeout(),120000)),prompts().get("intentPrompt"),prompts().get("decisionPrompt"),true,request.getExtraBody());
			}
		}
		String column = "LLM".equals(type) ? "llm_config_id" : "speech_config_id";
		List<Long> ids = userId == null ? List.of() : jdbcTemplate.query("select " + column + " from blade_smart_user_preference where user_id=?", (rs,n)->(Long)rs.getObject(1), userId);
		AiRuntimeConfig config = ids.isEmpty() || ids.get(0)==null ? null : currentInternal(type,ids.get(0),true);
		if(config==null)config=currentInternal(type,null,true);
		if (config == null) {
			throw new IllegalStateException("尚未配置并启用" + ("SPEECH".equals(type)?"语音":"LLM") + "服务");
		}
		return config;
	}

	@Transactional(rollbackFor = Exception.class)
	public Long save(AiConfigRequest request) {
		if (request == null || Func.isBlank(request.getConfigName()) || Func.isBlank(request.getBaseUrl())
			|| Func.isBlank(request.getModelName())) {
			throw new IllegalArgumentException("配置名称、服务地址和模型名称不能为空");
		}
		Long id = request.getId() == null ? IdWorker.getId() : request.getId();
		String type = configType(request.getConfigType());
        jdbcTemplate.queryForList("select config_type from blade_smart_model_policy where config_type=? for update", type);
		ModelBody.parse(request.getExtraBody());
		if(request.getModelAlias()!=null && request.getModelAlias().trim().length()>100)throw new IllegalArgumentException("模型别名不能超过100字");
		if(Boolean.TRUE.equals(request.getSystemDefault())&&!Boolean.TRUE.equals(request.getEnabled()))throw new IllegalArgumentException("系统默认模型必须启用");
		if(request.getId()!=null && !Boolean.TRUE.equals(request.getEnabled()) && jdbcTemplate.queryForObject("select count(*) from blade_ai_service_config where id=? and system_default=1 and is_deleted=0",Integer.class,id)>0)
			throw new IllegalArgumentException("请先将其他已启用模型设为系统默认，再停用此模型");
		if(request.getId()!=null && currentInternal(type,id,false)==null)throw new IllegalArgumentException("配置不存在或模型类型不匹配");
		String apiKey = request.getApiKey();
		if (request.getId() != null && (Func.isBlank(apiKey) || apiKey.contains("****"))) {
			List<String> values = jdbcTemplate.query(
				"select api_key from blade_ai_service_config where id=? and is_deleted=0",
				(rs, rowNum) -> rs.getString(1), request.getId());
			apiKey = values.isEmpty() ? "" : values.get(0);
		} else {
			apiKey = secretCodec.encrypt(apiKey);
		}
		int exists = jdbcTemplate.queryForObject(
			"select count(*) from blade_ai_service_config where id=?", Integer.class, id);
		if (exists > 0) {
			jdbcTemplate.update("update blade_ai_service_config set config_name=?,base_url=?,api_key=?,model_name=?,context_window=?,max_input_tokens=?,max_tokens=?,temperature=?,reasoning_effort=?,show_thinking=?,request_timeout=?,intent_prompt=?,decision_prompt=?,enabled=?,update_user=?,update_time=now() where id=?",
				request.getConfigName().trim(), normalizeUrl(request.getBaseUrl(),type), apiKey, request.getModelName().trim(),
				defaultInt(request.getContextWindow(), 1048576), defaultInt(request.getMaxInputTokens(), 991000),
				defaultInt(request.getMaxTokens(), 131072), defaultDecimal(request.getTemperature()), defaultReasoningEffort(request.getReasoningEffort()),
				!Boolean.FALSE.equals(request.getShowThinking()) ? 1 : 0,
				defaultInt(request.getRequestTimeout(), 120000), request.getIntentPrompt(), request.getDecisionPrompt(),
				Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0, AuthUtil.getUserId(), id);
		} else {
			jdbcTemplate.update("insert into blade_ai_service_config(id,config_name,base_url,api_key,model_name,context_window,max_input_tokens,max_tokens,temperature,reasoning_effort,show_thinking,request_timeout,intent_prompt,decision_prompt,enabled,create_user,create_time,update_user,update_time,is_deleted) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now(),?,now(),0)",
				id, request.getConfigName().trim(), normalizeUrl(request.getBaseUrl(),type), apiKey, request.getModelName().trim(),
				defaultInt(request.getContextWindow(), 1048576), defaultInt(request.getMaxInputTokens(), 991000),
				defaultInt(request.getMaxTokens(), 131072), defaultDecimal(request.getTemperature()), defaultReasoningEffort(request.getReasoningEffort()),
				!Boolean.FALSE.equals(request.getShowThinking()) ? 1 : 0,
				defaultInt(request.getRequestTimeout(), 120000), request.getIntentPrompt(), request.getDecisionPrompt(),
				Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0, AuthUtil.getUserId(), AuthUtil.getUserId());
		}
		jdbcTemplate.update("update blade_ai_service_config set config_type=?,extra_body=? where id=?",type,request.getExtraBody(),id);
		jdbcTemplate.update("update blade_ai_service_config set model_alias=? where id=?",Func.isBlank(request.getModelAlias())?request.getConfigName().trim():request.getModelAlias().trim(),id);
		if(Boolean.TRUE.equals(request.getSystemDefault()))setDefault(type,id);
		else ensureDefault(type);
		return id;
	}

	private AiRuntimeConfig currentInternal(String type, Long id, boolean enabledOnly) {
        Map<String,String> prompts = prompts();
		String sql = "select id,config_name,base_url,api_key,model_name,context_window,max_input_tokens,max_tokens,temperature,reasoning_effort,show_thinking,request_timeout,intent_prompt,decision_prompt,enabled,extra_body from blade_ai_service_config where is_deleted=0"
			+ " and config_type=?" + (id==null?"":" and id=?") + (enabledOnly ? " and enabled=1" : "") + " order by enabled desc,system_default desc,update_time desc,id desc limit 1";
		List<AiRuntimeConfig> list = jdbcTemplate.query(sql, (rs, rowNum) -> new AiRuntimeConfig(
			rs.getLong("id"), rs.getString("config_name"), rs.getString("base_url"),
			secretCodec.decrypt(rs.getString("api_key")), rs.getString("model_name"),
			rs.getInt("context_window"), rs.getInt("max_input_tokens"), rs.getInt("max_tokens"),
			rs.getBigDecimal("temperature"), rs.getString("reasoning_effort"), rs.getBoolean("show_thinking"), rs.getInt("request_timeout"),
			prompts.get("intentPrompt"), prompts.get("decisionPrompt"), rs.getBoolean("enabled"),rs.getString("extra_body")), id==null?new Object[]{type}:new Object[]{type,id});
		return list.isEmpty() ? null : list.get(0);
	}


    public Map<String,String> prompts() {
        List<Map<String,Object>> rows=jdbcTemplate.queryForList("select intent_prompt,decision_prompt from blade_smart_model_policy where config_type='LLM'");
        Map<String,Object> row=rows.isEmpty()?Map.of():rows.get(0);
        return Map.of("intentPrompt",effectiveIntentPrompt((String)row.get("intent_prompt")),"decisionPrompt",effectiveDecisionPrompt((String)row.get("decision_prompt")));
    }
    @Transactional(rollbackFor=Exception.class)
    public void savePrompts(Map<String,String> request) {
        String intent=request.get("intentPrompt"),decision=request.get("decisionPrompt");
        if(intent==null||decision==null||intent.isBlank()||decision.isBlank()||intent.length()>100000||decision.length()>100000)throw new IllegalArgumentException("请填写有效的全局提示词（每项不超过100000字）");
        jdbcTemplate.update("update blade_smart_model_policy set intent_prompt=?,decision_prompt=?,update_time=now() where config_type='LLM'",intent,decision);
    }
    @Transactional(rollbackFor=Exception.class)
    public void activate(String type, Long id) {
        type=configType(type);
        jdbcTemplate.queryForList("select config_type from blade_smart_model_policy where config_type=? for update",type);
        if(id==null||currentInternal(type,id,false)==null)throw new IllegalArgumentException("配置不存在或类型不匹配");
        jdbcTemplate.update("update blade_ai_service_config set enabled=1 where id=?",id);
        ensureDefault(type);
    }

    @Transactional(rollbackFor=Exception.class)
    public void setDefault(String type,Long id) {
        type=configType(type);
        jdbcTemplate.queryForList("select config_type from blade_smart_model_policy where config_type=? for update",type);
        if(id==null||currentInternal(type,id,true)==null)throw new IllegalArgumentException("请先启用该类型的模型");
        jdbcTemplate.update("update blade_ai_service_config set system_default=case when id=? then 1 else 0 end where config_type=? and is_deleted=0",id,type);
    }

    private void ensureDefault(String type) {
        if(jdbcTemplate.queryForObject("select count(*) from blade_ai_service_config where config_type=? and enabled=1 and system_default=1 and is_deleted=0",Integer.class,type)>0)return;
        var ids=jdbcTemplate.query("select id from blade_ai_service_config where config_type=? and enabled=1 and is_deleted=0 order by update_time desc,id desc limit 1",(rs,n)->rs.getLong(1),type);
        if(!ids.isEmpty())setDefault(type,ids.get(0));
    }

	public String configType(String type) {
		String value=Func.isBlank(type)?"LLM":type.trim().toUpperCase(java.util.Locale.ROOT);
		if(!java.util.Set.of("LLM","SPEECH").contains(value))throw new IllegalArgumentException("不支持的模型类型");
		return value;
	}

	/** Safe for app users: never return provider URL, key, or prompts. */
	public List<Map<String,Object>> choices(boolean enabledOnly) {
		return jdbcTemplate.query("select id,config_name,model_name,model_alias,system_default,config_type,enabled from blade_ai_service_config where is_deleted=0"+(enabledOnly?" and enabled=1":"")+" order by config_type,system_default desc,update_time desc,id desc",(rs,n)->{
			Map<String,Object> row=new LinkedHashMap<>();row.put("id",Long.toString(rs.getLong("id")));row.put("configName",rs.getString("config_name"));row.put("modelName",rs.getString("model_name"));row.put("modelAlias",Func.isBlank(rs.getString("model_alias"))?rs.getString("config_name"):rs.getString("model_alias"));row.put("systemDefault",rs.getBoolean("system_default"));row.put("configType",rs.getString("config_type"));row.put("enabled",rs.getBoolean("enabled"));return row;
		});
	}

	public Map<String,Object> preferences(Long userId) {
		Map<String,Object> result=new LinkedHashMap<>();result.put("models",choices(true));
		List<Map<String,Object>> rows=jdbcTemplate.query("select llm_config_id,speech_config_id from blade_smart_user_preference where user_id=?",(rs,n)->{
			Map<String,Object> row=new LinkedHashMap<>();row.put("llmConfigId",rs.getString(1));row.put("speechConfigId",rs.getString(2));return row;
		},userId);
		if(!rows.isEmpty())result.putAll(rows.get(0));
		List<Map<String,Object>> personal=jdbcTemplate.queryForList("select llm_mode,speech_mode,llm_personal,speech_personal,language from blade_smart_user_preference where user_id=?",userId);
		Map<String,Object> row=personal.isEmpty()?Map.of():personal.get(0);
		result.put("llmMode",row.getOrDefault("llm_mode","SYSTEM"));result.put("speechMode",row.getOrDefault("speech_mode","SYSTEM"));result.put("language",row.getOrDefault("language","zh-cn"));
		result.put("llm",maskedPersonal((String)row.get("llm_personal")));result.put("speech",maskedPersonal((String)row.get("speech_personal")));
		return result;
	}

	@Transactional(rollbackFor=Exception.class)
	public Map<String,Object> savePreferences(Long userId, org.springblade.modules.smartreminder.dto.SmartReminderDtos.ModelPreferenceRequest request) {
		if(request==null)throw new IllegalArgumentException("请选择模型");
		if(request.getLlmMode()!=null||request.getSpeechMode()!=null){return savePersonalPreferences(userId,request);}
		validateChoice(request.getLlmConfigId(),"LLM");validateChoice(request.getSpeechConfigId(),"SPEECH");
		jdbcTemplate.update("insert into blade_smart_user_preference(user_id,llm_config_id,speech_config_id,update_time) values(?,?,?,now()) on duplicate key update llm_config_id=values(llm_config_id),speech_config_id=values(speech_config_id),update_time=now()",userId,request.getLlmConfigId(),request.getSpeechConfigId());
		return preferences(userId);
	}

	private AiConfigRequest decodePersonal(String value){
		if(value==null||value.isBlank())return null;
		try{return new ObjectMapper().readValue(secretCodec.decrypt(value),AiConfigRequest.class);}catch(Exception e){throw new IllegalStateException("无法读取个人模型配置");}
	}
	private Map<String,Object> maskedPersonal(String value){
		AiConfigRequest request=decodePersonal(value);if(request==null)return Map.of();
		Map<String,Object> result=new LinkedHashMap<>();result.put("baseUrl",request.getBaseUrl());result.put("modelName",request.getModelName());result.put("apiKey",mask(request.getApiKey()));result.put("extraBody",request.getExtraBody());result.put("showThinking",request.getShowThinking());return result;
	}
	private String personalValue(AiConfigRequest request,String existing,String type,boolean required){
		if(request==null){if(required&&decodePersonal(existing)==null)throw new IllegalArgumentException("请填写个人模型配置");return existing;}
		AiConfigRequest previous=decodePersonal(existing);
		boolean noFields=Func.isBlank(request.getBaseUrl())&&Func.isBlank(request.getModelName())&&Func.isBlank(request.getApiKey());
		if(noFields&&!required)return existing;
		if(Func.isBlank(request.getBaseUrl())||Func.isBlank(request.getModelName()))throw new IllegalArgumentException("个人模型的地址和模型名不能为空");
		if(Func.isBlank(request.getApiKey())||request.getApiKey().contains("****"))request.setApiKey(previous==null?"":previous.getApiKey());
		if(Func.isBlank(request.getApiKey()))throw new IllegalArgumentException("请填写个人模型 Key");
		request.setBaseUrl(normalizeUrl(request.getBaseUrl(),type));PersonalModelEndpoint.validate(request.getBaseUrl());ModelBody.parse(request.getExtraBody());
		request.setConfigType(type);request.setId(null);request.setIntentPrompt(null);request.setDecisionPrompt(null);
		try{return secretCodec.encrypt(new ObjectMapper().writeValueAsString(request));}catch(Exception e){throw new IllegalStateException("保存个人模型失败");}
	}
	private Map<String,Object> savePersonalPreferences(Long userId,org.springblade.modules.smartreminder.dto.SmartReminderDtos.ModelPreferenceRequest request){
		String llm=request.getLlmMode(),speech=request.getSpeechMode(),language=request.getLanguage();
		if(!java.util.Set.of("SYSTEM","PERSONAL").contains(llm==null?"":llm)||!java.util.Set.of("SYSTEM","PERSONAL").contains(speech==null?"":speech)||!java.util.Set.of("zh-cn","en-us").contains(language==null?"":language))throw new IllegalArgumentException("模型来源或语言不正确");
		List<Map<String,Object>> rows=jdbcTemplate.queryForList("select llm_personal,speech_personal from blade_smart_user_preference where user_id=?",userId);Map<String,Object> old=rows.isEmpty()?Map.of():rows.get(0);
		String llmConfig=personalValue(request.getLlm(),(String)old.get("llm_personal"),"LLM",llm.equals("PERSONAL"));
		String speechConfig=personalValue(request.getSpeech(),(String)old.get("speech_personal"),"SPEECH",speech.equals("PERSONAL"));
		Long llmId="SYSTEM".equals(llm)?request.getLlmConfigId():null,speechId="SYSTEM".equals(speech)?request.getSpeechConfigId():null;
		validateChoice(llmId,"LLM");validateChoice(speechId,"SPEECH");
		jdbcTemplate.update("insert into blade_smart_user_preference(user_id,llm_mode,speech_mode,llm_personal,speech_personal,language,llm_config_id,speech_config_id,update_time) values(?,?,?,?,?,?,?,?,now()) on duplicate key update llm_mode=values(llm_mode),speech_mode=values(speech_mode),llm_personal=values(llm_personal),speech_personal=values(speech_personal),language=values(language),llm_config_id=values(llm_config_id),speech_config_id=values(speech_config_id),update_time=now()",userId,llm,speech,llmConfig,speechConfig,language,llmId,speechId);
		return preferences(userId);
	}
	public String languageInstruction(Long userId){
		List<String> languages=jdbcTemplate.query("select language from blade_smart_user_preference where user_id=?",(rs,n)->rs.getString(1),userId);
		return !languages.isEmpty()&&"en-us".equals(languages.get(0))?"\nOutput user-facing replies in English. Keep all protocol JSON keys and enum values unchanged. Do not translate proper names or original quoted messages.":"";
	}

	private void validateChoice(Long id,String type) {
		if(id!=null && currentInternal(type,id,true)==null)throw new IllegalArgumentException("选择的模型未启用或类型不匹配，请重新选择");
	}

	/** 将早期随系统保存的默认提示词自动升级；明确自定义的新提示词仍原样保留。 */
	private String effectiveIntentPrompt(String value) {
		if (Func.isBlank(value)) return SmartReminderPrompts.INTENT;
		if (value.contains("你是AI智能提醒系统的意图识别与首次评估规划器")
			&& (!value.contains("update_event") || !value.contains("stop_event")
			|| !value.contains("信息充分性与歧义判断规则") || !value.contains("只有系统工具成功新增分支")
			|| !value.contains("permissionMode") || !value.contains("feedbacks") || !value.contains("本轮待回复消息")
			|| !value.contains("批量操作协议") || !value.contains("人员别称记忆协议"))) return SmartReminderPrompts.INTENT;
		return value;
	}

	private String effectiveDecisionPrompt(String value) {
		if (Func.isBlank(value)) return SmartReminderPrompts.DECISION;
		if (value.contains("你是AI智能提醒系统的滚动评估器")
			&& (!value.contains("timeConflict") || !value.contains("同一接收人的其他活动事件"))) return SmartReminderPrompts.DECISION;
		return value;
	}

	private String mask(String value) {
		if (Func.isBlank(value)) return "";
		if (value.length() <= 8) return "****";
		return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
	}

	private String normalizeUrl(String value, String type) {
		String url = value.trim();
		java.net.URI uri=java.net.URI.create(url);
		if(!java.util.Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException("接口地址格式不正确");
		if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
		String endpoint="SPEECH".equals(type)?"/audio/transcriptions":"/chat/completions";
		if (!url.endsWith(endpoint)) {
			url += url.endsWith("/v1") ? endpoint : "/v1"+endpoint;
		}
		return url;
	}

	private int defaultInt(Integer value, int fallback) {
		return value == null || value <= 0 ? fallback : value;
	}

	private BigDecimal defaultDecimal(BigDecimal value) {
		return value == null ? new BigDecimal("0.30") : value;
	}

	private String defaultReasoningEffort(String value) {
		return Func.isBlank(value) ? "xhigh" : value.trim();
	}

	public record AiRuntimeConfig(Long id, String configName, String baseUrl, String apiKey,
		String modelName, int contextWindow, int maxInputTokens, int maxTokens, BigDecimal temperature,
		String reasoningEffort, boolean showThinking, int requestTimeout,
		String intentPrompt, String decisionPrompt, boolean enabled,String extraBody) {
		public AiRuntimeConfig(Long id,String configName,String baseUrl,String apiKey,String modelName,int contextWindow,int maxInputTokens,int maxTokens,BigDecimal temperature,String reasoningEffort,boolean showThinking,int requestTimeout,String intentPrompt,String decisionPrompt,boolean enabled){this(id,configName,baseUrl,apiKey,modelName,contextWindow,maxInputTokens,maxTokens,temperature,reasoningEffort,showThinking,requestTimeout,intentPrompt,decisionPrompt,enabled,"{}");}
	}
}
