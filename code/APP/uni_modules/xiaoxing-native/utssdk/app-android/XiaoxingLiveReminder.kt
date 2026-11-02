package uts.sdk.modules.xiaoxingNative

import android.app.*
import android.content.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/** Ordinary task reminders use a dynamic lock-screen card, not Android's promoted ongoing activity API. */
object XiaoxingLiveReminder {
    const val CHANNEL = "xiaoxing.upcoming.v1"
    private const val ID = 7110
    private const val PREFS = "xiaoxing.live-reminders.v1"
    private var shownKey = ""
    private var shownOwner = ""
    private val expiryHandler = Handler(Looper.getMainLooper())
    private var expiryTask: Runnable? = null
    private var dueTask: Runnable? = null

    @Synchronized fun clear(context: Context) {
        expiryTask?.let { expiryHandler.removeCallbacks(it) }; expiryTask=null
        dueTask?.let { expiryHandler.removeCallbacks(it) }; dueTask=null
        XiaoxingNotifications.manager(context).cancel(ID)
        shownKey = ""; shownOwner = ""
    }
    fun refresh(context: Context, sessionSnapshot: String, config: JSONObject, valid: () -> Boolean) {
        val session = JSONObject(sessionSnapshot)
        val connection = URL(config.getString("baseUrl") + "/app/push/live/snapshot").openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"; connection.connectTimeout = 8000; connection.readTimeout = 8000
            connection.setRequestProperty("Authorization", config.getString("basicAuth"))
            connection.setRequestProperty("Tenant-Id", config.getString("tenant"))
            connection.setRequestProperty("Blade-Requested-With", "BladeHttpRequest")
            connection.setRequestProperty("Blade-Auth", "bearer " + session.getString("token"))
            if (connection.responseCode == 401) { if (valid()) clear(context); return }
            // Older servers do not have this additive endpoint; retain ordinary push notifications.
            if (connection.responseCode != 200) return
            val result = connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            if (result.optInt("code", 200) != 200 || !valid()) return
            val data = result.optJSONObject("data") ?: return
            if (data.optString("owner") != session.optString("userId")) return
            render(context, data, sessionSnapshot, config.optString("language",if(Locale.getDefault().language=="zh") "zh-cn" else "en-us"), valid)
        } finally { connection.disconnect() }
    }
    @Synchronized private fun render(context: Context, data: JSONObject, snapshot: String, language: String, valid: () -> Boolean) {
        if (!valid() || XiaoxingAndroid.read(context,"session") != snapshot) return
        val owner = data.getString("owner")
        if (shownOwner != owner) clear(context)
        val now = data.getLong("serverTime")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit()
        prefs.all.forEach { (key,value) -> if (value is Long && value < now) edit.remove(key) }
        edit.apply()
        val items = data.optJSONArray("items") ?: return
        val eligible = (0 until items.length()).map { items.getJSONObject(it) }.filter {
            it.optString("owner") == owner && it.optLong("endAt") > now && !prefs.contains(owner + ":" + it.optString("key"))
        }
        val row = eligible.firstOrNull { it.optString("key") == shownKey } ?: eligible.firstOrNull()
        if (row == null) { clear(context); return }
        val manager = XiaoxingNotifications.manager(context)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "即将到来的提醒", NotificationManager.IMPORTANCE_LOW))
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) { clear(context); return }
        val key = row.getString("key")
        val target = row.getLong("targetAt")
        val end = row.getLong("endAt")
        val en = language == "en-us"
        val format = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone=TimeZone.getTimeZone("Asia/Shanghai") }
        val status = if (target <= now) { if(en) "Time to handle this task" else "已到时间，待处理" }
            else (if(row.optString("kind")=="DEADLINE") {if(en) "Due " else "截止 "} else {if(en) "Starts " else "事项时间 "}) + format.format(Date(target*1000))
        val open = Intent(context,XiaoxingNotificationOpenActivity::class.java)
            .putExtra("xiaoxing.owner",owner).putExtra("xiaoxing.event",row.getString("eventId")).putExtra("xiaoxing.type","LIVE_REMINDER")
        val tap = PendingIntent.getActivity(context,ID,open,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val dismiss = Intent(context,XiaoxingLiveDismissReceiver::class.java)
            .putExtra("owner",owner).putExtra("key",key).putExtra("end",end)
            .setData(android.net.Uri.parse("xiaoxing://dismiss/$owner/$key"))
        val deleted = PendingIntent.getBroadcast(context,ID,dismiss,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if(Build.VERSION.SDK_INT>=26) Notification.Builder(context,CHANNEL) else Notification.Builder(context)
        builder.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(row.getString("title"))
            .setContentText(status).setStyle(Notification.BigTextStyle().bigText(row.getString("title")+"\n"+status))
            .setContentIntent(tap).setDeleteIntent(deleted).setOnlyAlertOnce(true).setAutoCancel(false)
            .setCategory(Notification.CATEGORY_REMINDER).setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPriority(Notification.PRIORITY_LOW).setWhen(target*1000).setShowWhen(true)
        val custom = XiaoxingLiveCard.apply(context, builder, row, now, en)
        // A system chronometer otherwise counts past zero if our process dies.
        // Expire the countdown at its target; the local transition or next snapshot shows the due card.
        if(Build.VERSION.SDK_INT>=26) builder.setTimeoutAfter((if(custom && target>now) minOf(target,end)-now else end-now)*1000)
        if(!valid() || XiaoxingAndroid.read(context,"session")!=snapshot) return
        manager.notify(ID,builder.build());shownKey=key;shownOwner=owner
        dueTask?.let { expiryHandler.removeCallbacks(it) }; dueTask=null
        if(target>now && target<end) {
            val transitionData=JSONObject(data.toString())
            val scheduledAt=SystemClock.elapsedRealtime()
            dueTask=Runnable { synchronized(this) {
                // Handler callbacks can be delayed by sleep; never revive an already expired card.
                val currentTime=now+(SystemClock.elapsedRealtime()-scheduledAt)/1000
                if(shownOwner==owner && shownKey==key) render(context.applicationContext,transitionData.put("serverTime",currentTime),snapshot,language,valid)
            } }
            expiryHandler.postDelayed(dueTask!!,(target-now)*1000)
        }
        // Also expire on Android 6/7 and on network failure, without waiting for another server response.
        expiryTask?.let { expiryHandler.removeCallbacks(it) }
        val application = context.applicationContext
        expiryTask=Runnable { synchronized(this) { if(shownOwner==owner && shownKey==key) clear(application) } }
        expiryHandler.postDelayed(expiryTask!!,(end-now)*1000)
    }
    @Synchronized fun dismiss(context: Context,intent: Intent) {
        val owner=intent.getStringExtra("owner")?:return
        val key=intent.getStringExtra("key")?:return
        if(!key.matches(Regex("[a-f0-9]{40}"))) return
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putLong(owner+":"+key,intent.getLongExtra("end",0)).apply()
        if(shownOwner==owner && shownKey==key) {shownKey="";shownOwner=""}
    }
}

class XiaoxingLiveDismissReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {XiaoxingLiveReminder.dismiss(context,intent)}
}
