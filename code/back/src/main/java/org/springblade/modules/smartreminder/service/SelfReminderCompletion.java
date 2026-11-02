package org.springblade.modules.smartreminder.service;

/** Conservative fallback for an explicit current-task completion followed by no-more-reminders. */
final class SelfReminderCompletion {
    private SelfReminderCompletion() {}
    static boolean explicit(String source) {
        if (source == null) return false;
        String text = source.replaceAll("\\s+", "");
        // Questions, quotations, future conditions and partial progress require the normal AI review.
        if (text.matches("(?s).*(如果|要是|等我|等到|完成后|完成了再|完成的话|还没|尚未|未完成|没有完成|没完成|部分|一部分|一项|其中|还要|还需|仍需|继续提醒|不是|不要停止|是否|是不是|吗|？|\\?|[\"“”「」]).*")) return false;
        return text.matches("(?s).*(已经完成|已完成|完成了|已经办完|已办完|办完了|已经处理完|已处理完|做完了).*")
            && text.matches("(?s).*(不用再?提醒|不需要再?提醒|无需再?提醒|别再提醒|停止提醒|取消提醒).*");
    }
}
