package uts.sdk.modules.xiaoxingNative

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import org.json.JSONObject

/** Explicit alarm controls, also reachable from the notification while locked. */
class XiaoxingAlarmActivity : Activity() {
    private var alarm = JSONObject()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var stop: Button
    private fun ownAlarm() = alarm.optString("owner").isNotEmpty() && alarm.optString("owner") == XiaoxingAlarms.sessionOwner(this)
    private fun ringing() = ownAlarm() && XiaoxingAlarmService.currentId == XiaoxingAlarms.identity(alarm)
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        render()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); render() }
    private fun render() {
        handler.removeCallbacksAndMessages(null)
        alarm = try { JSONObject(intent.getStringExtra("alarm") ?: "{}") } catch (_: Exception) { JSONObject() }
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(28), dp(40), dp(28), dp(40)); setBackgroundColor(Color.rgb(241,245,255)) }
        fun label(text: String, size: Float) = TextView(this).apply { this.text=text; textSize=size; gravity=Gravity.CENTER; setTextColor(Color.rgb(38,52,81)); setPadding(0,dp(12),0,dp(12)) }
        root.addView(label("小醒闹铃", 28f))
        root.addView(label(if (ownAlarm()) alarm.optString("title", "时间到了") else "闹铃已失效", 22f))
        status = label("", 16f); root.addView(status)
        stop = Button(this).apply {
            text="停止响铃"; textSize=20f
            setOnClickListener {
                if (ringing()) startService(Intent(this@XiaoxingAlarmActivity, XiaoxingAlarmService::class.java).setAction("STOP").putExtra("id",XiaoxingAlarms.identity(alarm)))
                status.text="闹铃已关闭"; isEnabled=false; text="已关闭"
            }
        }
        root.addView(stop, LinearLayout.LayoutParams(-1,dp(64)))
        root.addView(Button(this).apply {
            text="查看事件"
            setOnClickListener {
                if (ownAlarm()) {
                    XiaoxingNotifications.pendingTap=JSONObject().put("eventId",alarm.optString("eventId")).put("recipientUserId",alarm.optString("owner")).toString()
                    packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)) }
                }
                finish()
            }
        },LinearLayout.LayoutParams(-1,dp(56)))
        setContentView(root)
        val refresh = object: Runnable {
            override fun run() {
                val active=ringing(); status.text=if(active) "正在响铃" else "闹铃已结束"; stop.isEnabled=active; stop.text=if(active) "停止响铃" else "已关闭"
                handler.postDelayed(this,500)
            }
        }
        handler.post(refresh)
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
}
