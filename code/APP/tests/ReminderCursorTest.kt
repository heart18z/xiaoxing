import uts.sdk.modules.xiaoxingNative.ReminderCursor

fun main() {
    val cursor = ReminderCursor()
    check(cursor.select("alice", listOf("1" to "REMINDER")).isEmpty())
    check(cursor.select("alice", listOf("1" to "REMINDER", "2" to "REMINDER", "3" to "TEXT")) == setOf("2"))
    check(cursor.select("alice", listOf("2" to "REMINDER")).isEmpty())
    check(cursor.select("bob", listOf("2" to "REMINDER", "4" to "QUESTION")).isEmpty())
    check(cursor.select("bob", listOf("4" to "QUESTION", "5" to "EVENT_ASSIGNED")) == setOf("5"))
    check(cursor.select("bob", listOf("" to "REMINDER")).isEmpty())
    println("PASS: backlog suppression, duplicate suppression, account isolation, notification types")
}
