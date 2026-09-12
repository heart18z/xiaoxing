package org.springblade.modules.smartreminder.service;

/** Only ephemeral chat reads use this boundary. Event audit/history must remain unfiltered. */
public final class ChatContext {
    private ChatContext() { }
    // Queries using this predicate must alias the message table as m.
    public static final String VISIBLE = "m.id > coalesce((select p.chat_context_start_id from blade_smart_user_preference p where p.user_id=m.user_id),0)";
}
