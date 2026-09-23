package uts.sdk.modules.xiaoxingNative

import android.app.*
import android.content.*
import android.net.Uri
import android.os.*
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject

object XiaoxingAlarms {
    const val CHANNEL = "xiaoxing.alarms.v1"
    private const val STORE = "android-alarms"
    fun manager(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    fun read(context: Context): JSONObject = try { JSONObject(XiaoxingAndroid.read(context, STORE)) } catch (_: Exception) { JSONObject().put("owner", "").put("alarms", JSONArray()) }
    fun save(context: Context, data: JSONObject) { check(XiaoxingAndroid.write(context, STORE, data.toString())) { "无法保存闹铃，请重试" } }
    fun permitted(context: Context) = Build.VERSION.SDK_INT < 31 || manager(context).canScheduleExactAlarms()
    fun fullScreenAllowed(context: Context) = Build.VERSION.SDK_INT < 34 || XiaoxingNotifications.manager(context).canUseFullScreenIntent()
    fun status(context: Context, owner: String): JSONObject {
        val item=XiaoxingAlarmService.currentAlarm
        val ringing=owner.isNotEmpty() && item.optString("owner")==owner && XiaoxingAlarmService.currentId==identity(item)
        return JSONObject().put("supported",true).put("ringing",ringing)
            .put("alarm",if(ringing) JSONObject(item.toString()) else JSONObject())
            .put("fullScreenAllowed",fullScreenAllowed(context))
    }
    fun sessionOwner(context: Context): String = try { JSONObject(XiaoxingAndroid.read(context, "session")).optString("userId") } catch (_: Exception) { "" }
    fun identity(item: JSONObject) = item.optString("owner") + ":" + item.optString("eventId")
    fun pending(context: Context, item: JSONObject): PendingIntent {
        val intent = Intent(context, XiaoxingAlarmReceiver::class.java).setAction("xiaoxing.alarm.FIRE")
            .setData(Uri.parse("xiaoxing-alarm:" + Uri.encode(identity(item)))).putExtra("id", identity(item))
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun schedule(context: Context, item: JSONObject) {
        val open = Intent(context, XiaoxingNotificationOpenActivity::class.java)
            .putExtra("xiaoxing.event", item.optString("eventId")).putExtra("xiaoxing.owner", item.optString("owner"))
            .setData(Uri.parse("xiaoxing-alarm-open:" + Uri.encode(identity(item))))
        val show = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager(context).setAlarmClock(AlarmManager.AlarmClockInfo((item.getDouble("timestamp") * 1000).toLong(), show), pending(context, item))
    }
    @Synchronized fun handle(activity: Activity, action: String, input: String): String {
        val args = JSONObject(input)
        val owner = args.optString("owner")
        if (action in setOf("alarm.status","alarm.open","alarm.stop","alarm.screenPermission")) {
            check(owner.isNotEmpty() && owner==sessionOwner(activity)){"账号已变化，请重新打开事件"}
            if(action=="alarm.screenPermission" && Build.VERSION.SDK_INT>=34) {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+activity.packageName)))
            }
            if(action=="alarm.open" || action=="alarm.stop") {
                val current=status(activity,owner)
                check(current.optBoolean("ringing") && args.optString("id")==XiaoxingAlarmService.currentId){"这次闹铃已结束"}
                if(action=="alarm.stop") {
                    activity.stopService(Intent(activity,XiaoxingAlarmService::class.java))
                    return JSONObject().put("supported",true).put("ringing",false).toString()
                }
                activity.startActivity(Intent(activity,XiaoxingAlarmActivity::class.java).putExtra("alarm",XiaoxingAlarmService.currentAlarm.toString()))
            }
            return status(activity,owner).toString()
        }
        val data = read(activity)
        var items = data.optJSONArray("alarms") ?: JSONArray()
        if (action == "alarm.activate") {
            if (owner != data.optString("owner")) {
                for (i in 0 until items.length()) manager(activity).cancel(pending(activity, items.getJSONObject(i)))
                activity.stopService(Intent(activity, XiaoxingAlarmService::class.java))
                items = JSONArray(); data.put("owner", owner).put("alarms", items); save(activity, data)
            }
            return JSONObject().put("supported", true).toString()
        }
        check(owner.isNotEmpty() && owner == data.optString("owner") && owner == sessionOwner(activity)) { "账号已变化，请重新打开事件" }
        if (action == "alarm.list") {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (!permitted(activity)) item.put("active", false)
            }
            save(activity, data)
            return status(activity,owner).put("alarms", items).toString()
        }
        val event = args.optString("eventId")
        check(event.isNotEmpty()) { "缺少事件编号" }
        val kept = JSONArray()
        var previous: JSONObject? = null
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.optString("eventId") == event) previous = item else kept.put(item)
        }
        if (action == "alarm.cancel") {
            previous?.let { manager(activity).cancel(pending(activity, it)) }
            if (XiaoxingAlarmService.currentId == owner + ":" + event) activity.stopService(Intent(activity, XiaoxingAlarmService::class.java))
            data.put("alarms", kept); save(activity, data)
            return "{}"
        }
        check(action == "alarm.schedule") { "未知闹铃操作" }
        if (!permitted(activity)) {
            if (Build.VERSION.SDK_INT >= 31) activity.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + activity.packageName)))
            throw IllegalStateException("请允许闹钟和提醒权限，返回后再次开启闹铃")
        }
        check(XiaoxingNotifications.allowed(activity)) { "请先在设置中开启通知权限，再设置闹铃" }
        val timestamp = args.optDouble("timestamp")
        check(timestamp.isFinite() && timestamp * 1000 > System.currentTimeMillis() + 2000) { "请设置明确的未来响铃时间" }
        args.put("id", owner + ":" + event).put("active", true)
        schedule(activity, args)
        try { data.put("alarms", kept.put(args)); save(activity, data) }
        catch (error: Exception) {
            manager(activity).cancel(pending(activity, args))
            previous?.let { if (it.optBoolean("active")) schedule(activity, it) }
            throw error
        }
        return args.toString()
    }
    @Synchronized fun fire(context: Context, id: String): JSONObject? {
        val data = read(context)
        if (data.optString("owner").isEmpty() || data.optString("owner") != sessionOwner(context)) return null
        val items = data.optJSONArray("alarms") ?: return null
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (identity(item) == id && item.optBoolean("active")) {
                // Reject stale intents after an event is rescheduled.
                if (item.optDouble("timestamp") * 1000 > System.currentTimeMillis() + 2000) return null
                item.put("active", false); save(context, data); return item
            }
        }
        return null
    }
    @Synchronized fun restore(context: Context) {
        val data = read(context)
        if (!permitted(context) || data.optString("owner") != sessionOwner(context)) return
        val items = data.optJSONArray("alarms") ?: return
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.optBoolean("active") && item.optDouble("timestamp") * 1000 > System.currentTimeMillis()) schedule(context, item)
        }
    }
}

class XiaoxingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action != "xiaoxing.alarm.FIRE") { XiaoxingAlarms.restore(context); return }
            val item = XiaoxingAlarms.fire(context, intent.getStringExtra("id") ?: "") ?: return
            val service = Intent(context, XiaoxingAlarmService::class.java).putExtra("alarm", item.toString())
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service) else context.startService(service)
        } catch (error: Exception) { android.util.Log.w("XiaoxingAlarm", "Alarm delivery failed: " + error.javaClass.simpleName) }
    }
}
class XiaoxingAlarmService : Service() {
    companion object { @Volatile var currentId = ""; @Volatile var currentAlarm=JSONObject() }
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            if (intent.getStringExtra("id") == currentId && currentId.isNotEmpty()) {
                ringtone?.stop()
                android.widget.Toast.makeText(this, "闹铃已关闭", android.widget.Toast.LENGTH_SHORT).show()
                stopSelf()
            }
            return START_NOT_STICKY
        }
        try {
            val item = JSONObject(intent?.getStringExtra("alarm") ?: "{}")
            if (item.optString("owner") != XiaoxingAlarms.sessionOwner(this)) { stopSelf(); return START_NOT_STICKY }
            currentId = XiaoxingAlarms.identity(item)
            currentAlarm = item
            val manager = XiaoxingNotifications.manager(this)
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(XiaoxingAlarms.CHANNEL, "本机闹铃", NotificationManager.IMPORTANCE_HIGH)
                channel.setSound(null, null)
                channel.enableVibration(true); channel.vibrationPattern=longArrayOf(0,350,250,350)
                manager.createNotificationChannel(channel)
            }
            val stop = PendingIntent.getService(this, 7201, Intent(this, XiaoxingAlarmService::class.java).setAction("STOP").putExtra("id", currentId).setData(Uri.parse("xiaoxing-alarm-stop:" + Uri.encode(currentId))), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val open = PendingIntent.getActivity(this, 7201, Intent(this, XiaoxingAlarmActivity::class.java)
                .putExtra("alarm", item.toString()).setData(Uri.parse("xiaoxing-alarm-ring:" + Uri.encode(currentId))), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, XiaoxingAlarms.CHANNEL) else Notification.Builder(this)
            if (XiaoxingAlarms.fullScreenAllowed(this)) builder.setFullScreenIntent(open, true)
            val notification = builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("正在响铃 · AI小醒")
                .setContentText(item.optString("title", "时间到了") + " · 点击打开闹铃，或选择停止响铃").setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX).setOngoing(true).setContentIntent(open)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setStyle(Notification.BigTextStyle().bigText(item.optString("title", "时间到了") + "\n正在响铃，点击通知打开停止按钮"))
                .addAction(android.R.drawable.ic_media_pause, "停止响铃", stop).build()
            startForeground(7201, notification)
            handler.removeCallbacksAndMessages(null)
            ringtone?.stop(); wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "xiaoxing:alarm").apply { acquire(65000) }
            ringtone = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            ringtone?.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            if (Build.VERSION.SDK_INT >= 28) ringtone?.isLooping = true
            ringtone?.play()
            // An alarm triggered while our UI is visible should expose controls immediately.
            // Background/locked launches remain governed by the system full-screen permission.
            val process=ActivityManager.RunningAppProcessInfo(); ActivityManager.getMyMemoryState(process)
            if(process.importance==ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                try { startActivity(Intent(this,XiaoxingAlarmActivity::class.java).putExtra("alarm",item.toString())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
                catch(error:Exception) { android.util.Log.w("XiaoxingAlarm","Alarm screen unavailable: "+error.javaClass.simpleName) }
            }
            handler.postDelayed({ stopSelf() }, 60000)
        } catch (error: Exception) { android.util.Log.w("XiaoxingAlarm", "Alarm playback failed: " + error.javaClass.simpleName); stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); ringtone?.stop()
        wakeLock?.let { if (it.isHeld) it.release() }; currentId = ""; currentAlarm=JSONObject(); stopForeground(true); super.onDestroy()
    }
}
