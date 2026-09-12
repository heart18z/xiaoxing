package org.springblade.modules.smartreminder.service;

/** Wording constraints supplement both built-in and administrator-supplied prompts. */
public final class ReminderWording {
    private ReminderWording() { }
    public static final String RULES = """

        原意与简洁表达约束（适用于summary、recipientTasks、fact、reply、reminderContent）：
        以用户原话为依据，只合并明确的澄清、人员替换和后续修改，不添加用户没说的行动或承诺。
        “通知大家验收通过，可以安排尾款支付”是转达信息，不得改为要求大家“配合付款”，也不得仅根据财务身份推断“由财务负责付款”。
        “通知陈颖下午3点开会”就这样简短记录；“通知陈颖明天7点见客户”澄清早上后只补足早上，改到下午6点只改时间。
        未要求的“提前安排出行、尽快回复、做好准备、核对信息、配合相关事宜”等一律不加。
        保留“可以、需要、必须、前、截至”等原话的约束强度；“3点开会”不能写成“3点前开会”。
        简单事情用一句短句，复杂任务才分句；人员身份有助于区分时保留，不能据身份追加职责。
        日期优先用明确月日和上午/下午时间，避免冗长秒数；跨年要写年份，绝不删掉必要日期或精确时间。
        “收到”“ok”仅表示该回复本身，不扩写为“同意执行、没有冲突、没有调整需求、已经完成”。fact尽量保留用户原文。
        对已记录的普通回执简短答复“已记录。”，不复述整件事，也不播报内部AI评估时间。评估时间仍在结构化字段里正常规划。
        变更通知只说最新变更，例如“见客户改为9月11日下午6点。”，无必要不重述旧时间和确认过程。
        提醒时间的提前规划是系统内部策略，不等于给用户添加提前行动的要求。
        """;
    public static boolean isAcknowledgement(String text) {
        return text!=null && text.trim().replaceAll("[。.!！,，\\s]+$", "").matches("(?i)(收到|已收到|知道了|明白了?|了解了?|好的?|ok|okay)");
    }
}
