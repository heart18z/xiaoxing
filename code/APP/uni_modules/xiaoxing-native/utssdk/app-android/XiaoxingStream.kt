package uts.sdk.modules.xiaoxingNative

import android.app.Activity
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/** Read the response as it arrives; cancellation only closes observation, never the job. */
object XiaoxingStream {
    private class Watch { @Volatile var cancelled = false; @Volatile var connection: HttpURLConnection? = null }
    private val watches = ConcurrentHashMap<String, Watch>()
    private val executor = Executors.newCachedThreadPool()
    private val main = Handler(Looper.getMainLooper())
    @JvmStatic fun call(activity: Activity, action: String, input: String, callback: (String) -> Unit) {
        val data = JSONObject(input)
        val id = data.optString("id")
        if (action == "stream.cancel") {
            val watch = watches.remove(id)
            watch?.cancelled = true
            executor.execute { watch?.connection?.disconnect() }
            callback("{}")
            return
        }
        val watch = Watch()
        watches.put(id, watch)?.let { previous -> previous.cancelled = true; executor.execute { previous.connection?.disconnect() } }
        fun emit(value: JSONObject) { main.post { if (!watch.cancelled) callback(value.toString()) } }
        executor.execute {
            try {
                val url = URL(data.getString("url"))
                require(url.protocol == "https")
                val connection = url.openConnection() as HttpURLConnection
                watch.connection = connection
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 35000
                val headers = data.getJSONObject("headers")
                headers.keys().forEach { name -> connection.setRequestProperty(name, headers.getString(name)) }
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.doOutput = true
                connection.outputStream.use { it.write(data.getString("body").toByteArray(Charsets.UTF_8)) }
                check(connection.responseCode in 200..299) { "流式连接暂不可用（${connection.responseCode}）" }
                connection.inputStream.reader(Charsets.UTF_8).use { reader ->
                    val buffer = CharArray(2048)
                    while (!watch.cancelled) {
                        val count = reader.read(buffer)
                        if (count < 0) break
                        emit(JSONObject().put("text", String(buffer, 0, count)))
                    }
                }
                emit(JSONObject().put("ended", true))
            } catch (error: Exception) {
                emit(JSONObject().put("error", "流式连接中断，正在查询处理结果"))
            } finally { watch.connection?.disconnect(); watches.remove(id, watch) }
        }
    }
}
