package uts.sdk.modules.xiaoxingNative

// The first snapshot establishes a baseline; reopening never alerts old history.
class ReminderCursor {
    private var owner = ""
    private var primed = false
    private val seen = LinkedHashSet<String>()
    fun select(user: String, rows: List<Pair<String, String>>): Set<String> {
        if (owner != user) { owner = user; primed = false; seen.clear() }
        val fresh = LinkedHashSet<String>()
        for ((id, type) in rows) {
            if (id.isNotEmpty() && seen.add(id) && primed && type in setOf("REMINDER", "QUESTION", "FEEDBACK", "CONFLICT", "EVENT_ASSIGNED")) fresh.add(id)
        }
        primed = true
        while (seen.size > 500) seen.remove(seen.first())
        return fresh
    }
}
