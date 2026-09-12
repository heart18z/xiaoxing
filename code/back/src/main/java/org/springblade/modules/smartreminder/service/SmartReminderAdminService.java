package org.springblade.modules.smartreminder.service;

import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.utils.Func;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartReminderAdminService {

	private final JdbcTemplate jdbcTemplate;

	public Map<String, Object> dashboard() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("totalEvents", count("select count(*) from blade_smart_event"));
		data.put("activeEvents", count("select count(*) from blade_smart_event where event_status='ACTIVE'"));
		data.put("activeBranches", count("select count(*) from blade_smart_event_branch where branch_status='ACTIVE'"));
		data.put("dueBranches", count("select count(*) from blade_smart_event_branch where branch_status='ACTIVE' and next_evaluate_time<=now()"));
		data.put("totalNotifications", count("select count(*) from blade_smart_notification"));
		data.put("unreadNotifications", count("select count(*) from blade_smart_notification where read_time is null"));
		data.put("evaluationsToday", count("select count(*) from blade_smart_evaluation where create_time>=curdate()"));
		List<Map<String, Object>> recent = jdbcTemplate.queryForList("""
			select e.id,e.event_no as eventNo,e.event_summary as eventSummary,e.event_status as eventStatus,
			coalesce(u.real_name,u.name,u.account) as creatorName,e.event_time as eventTime,e.create_time as createTime,
			count(distinct b.id) as recipientCount,sum(case when b.branch_status='ACTIVE' then 1 else 0 end) as activeBranchCount
			from blade_smart_event e join blade_user u on u.id=e.creator_user_id
			left join blade_smart_event_branch b on b.event_id=e.id
			group by e.id order by e.create_time desc limit 10
			""");
		recent.forEach(this::normalizeIds);
		data.put("recentEvents", recent);
		return data;
	}

	public Map<String, Object> events(int page, int size, String keyword, String status) {
		PageSpec spec = pageSpec(page, size);
		List<Object> args = new ArrayList<>();
		StringBuilder where = new StringBuilder(" where 1=1");
		String value = Func.toStr(keyword).trim();
		if (!value.isBlank()) {
			where.append(" and (e.event_no like ? or e.event_summary like ? or u.account like ? or u.name like ? or u.real_name like ?)");
			String like = "%" + value + "%";
			for (int i = 0; i < 5; i++) args.add(like);
		}
		String state = Func.toStr(status).trim().toUpperCase(Locale.ROOT);
		if (List.of("ACTIVE", "STOPPED", "COMPLETED").contains(state)) {
			where.append(" and e.event_status=?");
			args.add(state);
		}
		long total = queryCount("select count(*) from blade_smart_event e join blade_user u on u.id=e.creator_user_id" + where, args);
		List<Object> pageArgs = new ArrayList<>(args);
		pageArgs.add(spec.size());
		pageArgs.add(spec.offset());
		List<Map<String, Object>> records = jdbcTemplate.queryForList("""
			select e.id,e.event_no as eventNo,e.event_summary as eventSummary,e.event_status as eventStatus,
			e.time_description as timeDescription,e.event_time as eventTime,e.deadline_time as deadlineTime,
			e.stop_reason as stopReason,(select t.content from blade_smart_timeline t where t.event_id=e.id order by t.create_time desc,t.id desc limit 1) as latestFact,
			(select t.create_time from blade_smart_timeline t where t.event_id=e.id order by t.create_time desc,t.id desc limit 1) as latestProgressTime,
			e.create_time as createTime,coalesce(u.real_name,u.name,u.account) as creatorName,
			count(distinct b.id) as recipientCount,sum(case when b.branch_status='ACTIVE' then 1 else 0 end) as activeBranchCount,
			count(distinct n.id) as notificationCount,count(distinct v.id) as evaluationCount
			from blade_smart_event e join blade_user u on u.id=e.creator_user_id
			left join blade_smart_event_branch b on b.event_id=e.id
			left join blade_smart_notification n on n.event_id=e.id
			left join blade_smart_evaluation v on v.event_id=e.id
			""" + where + " group by e.id order by e.create_time desc limit ? offset ?", pageArgs.toArray());
		records.forEach(this::normalizeIds);
		return page(total, spec, records);
	}

	public Map<String, Object> notifications(int page, int size, String keyword, String readStatus) {
		PageSpec spec = pageSpec(page, size);
		List<Object> args = new ArrayList<>();
		StringBuilder where = new StringBuilder(" where 1=1");
		String value = Func.toStr(keyword).trim();
		if (!value.isBlank()) {
			where.append(" and (e.event_no like ? or e.event_summary like ? or su.account like ? or ru.account like ? or n.notification_content like ?)");
			String like = "%" + value + "%";
			for (int i = 0; i < 5; i++) args.add(like);
		}
		if ("READ".equalsIgnoreCase(readStatus)) where.append(" and n.read_time is not null");
		if ("UNREAD".equalsIgnoreCase(readStatus)) where.append(" and n.read_time is null");
		String joins = " from blade_smart_notification n join blade_smart_event e on e.id=n.event_id join blade_user su on su.id=n.sender_user_id join blade_user ru on ru.id=n.recipient_user_id";
		long total = queryCount("select count(*)" + joins + where, args);
		List<Object> pageArgs = new ArrayList<>(args);
		pageArgs.add(spec.size());
		pageArgs.add(spec.offset());
		List<Map<String, Object>> records = jdbcTemplate.queryForList("""
			select n.id,n.event_id as eventId,e.event_no as eventNo,e.event_summary as eventSummary,
			coalesce(su.real_name,su.name,su.account) as senderName,coalesce(ru.real_name,ru.name,ru.account) as recipientName,
			n.notification_content as notificationContent,n.send_reason as sendReason,n.send_status as sendStatus,
			n.plan_time as planTime,n.sent_time as sentTime,n.read_time as readTime,
			case when n.read_time is not null then coalesce(ru.real_name,ru.name,ru.account) else null end as readerName
			""" + joins + where + " order by n.sent_time desc limit ? offset ?", pageArgs.toArray());
		records.forEach(this::normalizeIds);
		return page(total, spec, records);
	}

	public Map<String, Object> evaluations(int page, int size, String action) {
		PageSpec spec = pageSpec(page, size);
		List<Object> args = new ArrayList<>();
		StringBuilder where = new StringBuilder(" where 1=1");
		String state = Func.toStr(action).trim().toUpperCase(Locale.ROOT);
		if (List.of("SEND", "DEFER", "SKIP", "ASK_RECIPIENT", "ASK_CREATOR", "STOP").contains(state)) {
			where.append(" and v.decision_action=?");
			args.add(state);
		}
		String joins = " from blade_smart_evaluation v join blade_smart_event e on e.id=v.event_id join blade_smart_event_branch b on b.id=v.branch_id join blade_user u on u.id=b.recipient_user_id";
		long total = queryCount("select count(*)" + joins + where, args);
		List<Object> pageArgs = new ArrayList<>(args);
		pageArgs.add(spec.size());
		pageArgs.add(spec.offset());
		List<Map<String, Object>> records = jdbcTemplate.queryForList("""
			select v.id,v.event_id as eventId,e.event_no as eventNo,e.event_summary as eventSummary,
			coalesce(u.real_name,u.name,u.account) as recipientName,v.trigger_type as triggerType,
			v.decision_action as decisionAction,v.decision_reason as decisionReason,v.confidence,
			v.old_evaluate_time as oldEvaluateTime,v.new_evaluate_time as newEvaluateTime,
			v.model_name as modelName,v.prompt_version as promptVersion,v.create_time as createTime
			""" + joins + where + " order by v.create_time desc limit ? offset ?", pageArgs.toArray());
		records.forEach(this::normalizeIds);
		return page(total, spec, records);
	}

	private long count(String sql) {
		Long value = jdbcTemplate.queryForObject(sql, Long.class);
		return value == null ? 0L : value;
	}

	private long queryCount(String sql, List<Object> args) {
		Long value = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
		return value == null ? 0L : value;
	}

	private Map<String, Object> page(long total, PageSpec spec, List<Map<String, Object>> records) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("records", records);
		result.put("total", total);
		result.put("current", spec.page());
		result.put("size", spec.size());
		return result;
	}

	private PageSpec pageSpec(int page, int size) {
		int safePage = Math.max(1, page);
		int safeSize = Math.max(10, Math.min(size, 100));
		return new PageSpec(safePage, safeSize, (safePage - 1) * safeSize);
	}

	private void normalizeIds(Map<String, Object> row) {
		for (String key : new ArrayList<>(row.keySet())) {
			Object value = row.get(key);
			if (value instanceof Number && ("id".equalsIgnoreCase(key) || key.toLowerCase(Locale.ROOT).endsWith("id"))) {
				row.put(key, value.toString());
			}
		}
	}

	private record PageSpec(int page, int size, int offset) { }
}
