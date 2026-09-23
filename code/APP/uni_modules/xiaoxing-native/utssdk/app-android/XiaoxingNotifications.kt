package uts.sdk.modules.xiaoxingNative

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object XiaoxingNotifications {
    const val ALERT = "xiaoxing.reminders.v1"
    const val CONNECTION = "xiaoxing.background.v1"
    const val SERVICE_ID = 7101
    @Volatile var pendingTap = "{}"
    @Volatile var running = false
    @Volatile var lastError = ""
    @Volatile var expiredSession = ""
    fun manager(context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    fun channels(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            manager(context).createNotificationChannel(NotificationChannel(ALERT, "事件提醒", NotificationManager.IMPORTANCE_HIGH))
            manager(context).createNotificationChannel(NotificationChannel(CONNECTION, "后台接收", NotificationManager.IMPORTANCE_LOW))
        }
    }
    fun allowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) return false
        if (Build.VERSION.SDK_INT >= 24 && !manager(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || manager(context).getNotificationChannel(ALERT)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    fun state(context: Context): String = JSONObject().put("supported", true)
        .put("permission", if (allowed(context)) "authorized" else "denied")
        .put("registered", running).put("message", lastError).put("mode", "background").toString()
    fun start(context: Context) {
        channels(context)
        if (!allowed(context) || running) return
        val current = XiaoxingAndroid.read(context, "session")
        if (current.isEmpty() || current == expiredSession) return
        try {
            val intent = Intent(context, XiaoxingReminderService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
            lastError = ""
        } catch (_: Exception) { lastError = "后台接收未启动，请在应用内重新检查通知" }
    }
    fun stop(context: Context) {
        context.stopService(Intent(context, XiaoxingReminderService::class.java))
        manager(context).cancelAll()
        running = false
        pendingTap = "{}"
    }
    @Synchronized fun tap(): String {
        val result = pendingTap
        pendingTap = "{}"
        return result
    }
    fun notification(context: Context, title: String, text: String, event: String = "", owner: String = "", ongoing: Boolean = false): Notification {
        val intent = Intent(context, XiaoxingNotificationOpenActivity::class.java)
        intent.putExtra("xiaoxing.event", event).putExtra("xiaoxing.owner", owner)
        val pending = PendingIntent.getActivity(context, (owner + ":" + event).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, if (ongoing) CONNECTION else ALERT) else Notification.Builder(context)
        return builder.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(pending)
            .setAutoCancel(!ongoing).setOngoing(ongoing).setOnlyAlertOnce(ongoing)
            .setCategory(if (ongoing) Notification.CATEGORY_SERVICE else Notification.CATEGORY_REMINDER)
            .setPriority(if (ongoing) Notification.PRIORITY_LOW else Notification.PRIORITY_HIGH).build()
    }
    @JvmStatic fun call(activity: Activity, action: String, json: String, callback: (String) -> Unit) {
        activity.runOnUiThread {
            try {
                if (action.startsWith("alarm.")) {
                    try { callback(XiaoxingAlarms.handle(activity, action, json)) }
                    catch (error: Exception) { callback(JSONObject().put("error", error.message ?: "闹铃设置失败").toString()) }
                    return@runOnUiThread
                }
                when(action) {
                    "initialize" -> { channels(activity); callback("{\"supported\":true}") }
                    "push.status", "push.request" -> {
                        val input = JSONObject(json)
                        val base = input.optString("baseUrl")
                        if (!base.startsWith("https://")) { callback("{\"error\":\"通知服务地址无效\"}"); return@runOnUiThread }
                        if (!XiaoxingAndroid.write(activity, "android-push-config", json)) { callback("{\"error\":\"无法保存通知配置\"}"); return@runOnUiThread }
                        channels(activity)
                        if (action == "push.request" && Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                            if (activity.fragmentManager.findFragmentByTag("xiaoxing.notifications") != null) { callback(state(activity)); return@runOnUiThread }
                            val fragment = XiaoxingNotificationPermission(); fragment.callback = callback
                            activity.fragmentManager.beginTransaction().add(fragment, "xiaoxing.notifications").commitAllowingStateLoss()
                        } else { start(activity); callback(state(activity)) }
                    }
                    "push.tap" -> callback(tap())
                    "push.clear" -> { stop(activity); callback("{}") }
                    "settings" -> { activity.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)); callback("{}") }
                    else -> callback("{\"supported\":false}")
                }
            } catch (_: Exception) { callback("{\"error\":\"通知操作失败，请重新检查通知设置\"}") }
        }
    }
}

@Suppress("DEPRECATION")
class XiaoxingNotificationPermission: Fragment() {
    var callback: ((String) -> Unit)? = null
    override fun onCreate(state: Bundle?) { super.onCreate(state); retainInstance = true; if (state == null) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 7242) }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, grants: IntArray) {
        if (code != 7242) return
        activity?.let { XiaoxingNotifications.start(it); callback?.invoke(XiaoxingNotifications.state(it)) }
        callback = null
        fragmentManager?.beginTransaction()?.remove(this)?.commitAllowingStateLoss()
    }
}

class XiaoxingReminderService: Service() {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var owner = ""
    private val cursor = ReminderCursor()
    @Volatile private var closed = false
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        XiaoxingNotifications.channels(this)
        startForeground(XiaoxingNotifications.SERVICE_ID, XiaoxingNotifications.notification(this, "AI小醒正在后台接收提醒", "保持应用运行，可接收新的事件通知", ongoing = true))
        XiaoxingNotifications.running = true
        executor.scheduleWithFixedDelay({ poll() }, 0, 15, TimeUnit.SECONDS)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, id: Int): Int = START_NOT_STICKY
    override fun onTimeout(startId: Int, type: Int) { stopSelf() }
    override fun onDestroy() { closed = true; executor.shutdownNow(); XiaoxingNotifications.running = false; super.onDestroy() }
    private fun poll() {
        if (closed) return
        try {
            val snapshot = XiaoxingAndroid.read(this, "session")
            if (snapshot.isEmpty() || !XiaoxingNotifications.allowed(this)) { stopSelf(); return }
            val session = JSONObject(snapshot)
            val user = session.optString("userId")
            if (user.isEmpty() || session.optString("token").isEmpty()) { stopSelf(); return }
            if (owner.isNotEmpty() && owner != user) {
                val manager = XiaoxingNotifications.manager(this)
                manager.activeNotifications.filter { it.tag?.startsWith("xiaoxing:") == true }.forEach { manager.cancel(it.tag, it.id) }
            }
            owner = user
            val config = JSONObject(XiaoxingAndroid.read(this, "android-push-config"))
            val connection = URL(config.getString("baseUrl") + "/app/reminder/chat/sync?limit=150&revision=").openConnection() as HttpURLConnection
            val result: JSONObject
            try {
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"; connection.connectTimeout = 10000; connection.readTimeout = 10000
                connection.setRequestProperty("Authorization", config.getString("basicAuth"))
                connection.setRequestProperty("Tenant-Id", config.getString("tenant"))
                connection.setRequestProperty("Blade-Requested-With", "BladeHttpRequest")
                connection.setRequestProperty("Blade-Auth", "bearer " + session.getString("token"))
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                if (connection.responseCode == 401) { expired(snapshot); return }
                if (connection.responseCode != 200) return
                result = connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            } finally { connection.disconnect() }
            if (closed || XiaoxingAndroid.read(this, "session") != snapshot) return
            val code = result.optInt("code", 200)
            if (code != 200) {
                if (code == 401 || result.optString("msg").contains("令牌")) expired(snapshot)
                return
            }
            val rows = result.optJSONObject("data")?.optJSONArray("messages") ?: return
            val currentRows = (0 until rows.length()).map { rows.getJSONObject(it) }
            val fresh = cursor.select(user, currentRows.map { it.optString("id") to it.optString("messageType") })
            val pending = currentRows.filter { it.optString("id") in fresh }
            for (row in pending) {
                if (closed || XiaoxingAndroid.read(this, "session") != snapshot) return
                val text = row.optString("content").ifBlank { "你有一条新的事件提醒" }
                XiaoxingNotifications.manager(this).notify("xiaoxing:" + owner, row.getString("id").hashCode(),
                    XiaoxingNotifications.notification(this, "AI小醒 · 事件提醒", text, row.optString("eventId"), owner))
            }
            XiaoxingNotifications.lastError = ""
        } catch (_: Exception) {
            // Retry without logging response bodies or credentials.
            XiaoxingNotifications.lastError = "后台连接暂时中断，正在重试"
        }
    }
    private fun expired(snapshot: String) {
        if (closed || XiaoxingAndroid.read(this, "session") != snapshot) return
        XiaoxingNotifications.expiredSession = snapshot
        XiaoxingNotifications.lastError = "登录已过期，请打开应用重新登录后继续接收提醒"
        XiaoxingNotifications.manager(this).notify(7102, XiaoxingNotifications.notification(this, "AI小醒", XiaoxingNotifications.lastError))
        stopSelf()
    }
}

class XiaoxingNotificationOpenActivity: Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        XiaoxingNotifications.pendingTap = JSONObject()
            .put("eventId", intent.getStringExtra("xiaoxing.event") ?: "")
            .put("recipientUserId", intent.getStringExtra("xiaoxing.owner") ?: "").toString()
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(it)
        }
        finish()
    }
}
