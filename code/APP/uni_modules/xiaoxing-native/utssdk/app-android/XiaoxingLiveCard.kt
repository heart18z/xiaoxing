package uts.sdk.modules.xiaoxingNative

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Native lock-screen/notification views; no WebView or per-second network refresh. */
object XiaoxingLiveCard {
    // Resolve merged resources through the host package: HBuilder assigns the final R namespace.
    // res/raw/xiaoxing_live_keep.xml preserves the named layouts when shrinking resources.
    private fun resource(context: Context, type: String, name: String): Int =
        context.resources.getIdentifier(name, type, context.packageName)

    fun apply(context: Context, builder: Notification.Builder, row: JSONObject, now: Long, en: Boolean): Boolean {
        val small = resource(context,"layout","xiaoxing_live_compact")
        val expanded = resource(context,"layout","xiaoxing_live_expanded")
        val names = listOf("title","subtitle","brand","status","clock","time","time_label","time_unit")
        val ids = names.associateWith { resource(context,"id","xiaoxing_live_$it") }
        // Older custom bases might not contain the newly added native layouts.
        if (small == 0 || expanded == 0 || ids.values.any { it == 0 }) return false
        val target = row.getLong("targetAt")
        val due = target <= now
        val deadline = row.optString("kind") == "DEADLINE"
        // Pre-O cannot expire a notification if the process is killed. Show the absolute time there.
        val countdown = Build.VERSION.SDK_INT >= 26 && !due
        val zone = TimeZone.getTimeZone("Asia/Shanghai")
        val locale = if(en) Locale.US else Locale.SIMPLIFIED_CHINESE
        val format = SimpleDateFormat("HH:mm",locale).apply { timeZone=zone }
        val time = format.format(Date(target*1000))
        val day = Calendar.getInstance(zone).apply { timeInMillis=now*1000 }
        val targetDay = Calendar.getInstance(zone).apply { timeInMillis=target*1000 }
        fun sameDay(): Boolean = day.get(Calendar.YEAR)==targetDay.get(Calendar.YEAR) && day.get(Calendar.DAY_OF_YEAR)==targetDay.get(Calendar.DAY_OF_YEAR)
        val dateLabel = if(sameDay()) { if(en) "Today" else "今天" } else {
            day.add(Calendar.DAY_OF_YEAR,1)
            if(sameDay()) { if(en) "Tomorrow" else "明天" }
            else SimpleDateFormat(if(en) "MMM d" else "M月d日",locale).apply { timeZone=zone }.format(Date(target*1000))
        }
        val status = if(due) { if(en) "Due now" else "已到时间" }
            else if(deadline) { if(en) "Due soon" else "即将截止" } else { if(en) "Upcoming" else "即将提醒" }
        val subtitle = "$dateLabel $time · " + if(deadline) { if(en) "Deadline" else "截止" } else { if(en) "Reminder" else "提醒" }
        val base = SystemClock.elapsedRealtime() + (target-now).coerceAtLeast(0)*1000
        fun layout(layoutId: Int): RemoteViews = RemoteViews(context.packageName,layoutId).apply {
            setTextViewText(ids.getValue("title"),row.getString("title"))
            setTextViewText(ids.getValue("subtitle"),subtitle)
            setTextViewText(ids.getValue("brand"),if(en) "AI Xiaoxing" else "AI小醒")
            setTextViewText(ids.getValue("status"),"● $status")
            setTextViewText(ids.getValue("time_label"),if(due) { if(en) "Status" else "当前状态" }
                else if(!countdown) { if(en) "Reminder time" else "提醒时间" }
                else if(deadline) { if(en) "Time until due" else "距离截止" } else { if(en) "Time remaining" else "距离提醒" })
            setTextViewText(ids.getValue("time"),if(due) { if(en) "Due now" else "已到时间" } else time)
            setTextViewText(ids.getValue("time_unit"),if(en) "Tap to view" else "点击查看")
            setViewVisibility(ids.getValue("clock"),if(countdown) View.VISIBLE else View.GONE)
            setViewVisibility(ids.getValue("time"),if(countdown) View.GONE else View.VISIBLE)
            if(countdown && Build.VERSION.SDK_INT>=24) {
                setChronometerCountDown(ids.getValue("clock"),true)
                setChronometer(ids.getValue("clock"),base,null,true)
            }
        }
        val compact = layout(small)
        val big = layout(expanded)
        builder.setShowWhen(false).setUsesChronometer(false)
        if(Build.VERSION.SDK_INT>=24) {
            builder.setStyle(Notification.DecoratedCustomViewStyle())
                .setCustomContentView(compact).setCustomBigContentView(big)
        } else {
            builder.setStyle(null).setContent(compact)
            // API 23 has no setCustomBigContentView; the compact card remains fully functional.
        }
        return true
    }
}
