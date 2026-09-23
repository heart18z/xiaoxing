package uts.sdk.modules.xiaoxingNative

import android.app.Activity
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile

/** A single PCM capture supplies both the WAV upload and the visible microphone level. */
object XiaoxingRecorder {
    private class Capture(val id: String, val file: File) {
        @Volatile var stopping = false
        @Volatile var cancelled = false
        @Volatile var finished = false
        @Volatile var level = 0
        @Volatile var error = ""
    }
    @Volatile private var capture: Capture? = null

    @JvmStatic fun call(activity: Activity, action: String, input: String, callback: (String) -> Unit) {
        val data = JSONObject(input)
        val id = data.optString("session")
        if (action == "audio.start") {
            synchronized(this) {
                if (capture?.finished == false) { callback("{\"error\":\"上一段录音尚未结束，请稍后重试\"}"); return }
                val folder = File(activity.cacheDir, "xiaoxing-voice").apply { mkdirs() }
                folder.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000 }?.forEach { it.delete() }
                val current = Capture(id, File.createTempFile("voice-", ".wav", folder))
                capture = current
                Thread { record(current) }.start()
            }
            callback("{\"started\":true}")
            return
        }
        val current = capture
        if (current == null || current.id != id) { callback("{\"error\":\"录音已结束\"}"); return }
        if (action == "audio.stop") {
            current.cancelled = data.optBoolean("cancel")
            current.stopping = true
            if (current.cancelled && current.finished) current.file.delete()
        }
        callback(JSONObject().put("finished", current.finished).put("level", current.level)
            .put("error", current.error).put("path", if (current.finished && !current.cancelled) current.file.absolutePath else "").toString())
    }
    private fun record(current: Capture) {
        var recorder: AudioRecord? = null
        try {
            val size = maxOf(3200, AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
            recorder = AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size * 2)
            check(recorder.state == AudioRecord.STATE_INITIALIZED)
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            val samples = ShortArray(1600)
            val bytes = ByteArray(3200)
            var total = 0
            val start = System.currentTimeMillis()
            RandomAccessFile(current.file, "rw").use { out ->
                out.write(ByteArray(44))
                while (!current.stopping && System.currentTimeMillis() - start < 60000) {
                    val count = recorder.read(samples, 0, samples.size)
                    if (count < 0) throw IllegalStateException("Microphone read failed")
                    if (count == 0) continue
                    for (i in 0 until count) {
                        val value = samples[i].toInt()
                        bytes[i * 2] = value.toByte()
                        bytes[i * 2 + 1] = (value shr 8).toByte()
                    }
                    current.level = XiaoxingPcm.level(samples, count)
                    out.write(bytes, 0, count * 2)
                    total += count * 2
                }
                XiaoxingPcm.header(out, total)
            }
            if (total == 0 && !current.cancelled) current.error = "未录到声音，请重试"
        } catch (_: Exception) {
            current.error = "录音失败，请检查麦克风权限或是否被其他应用占用"
        } finally {
            try { recorder?.stop() } catch (_: Exception) { }
            recorder?.release()
            if (current.cancelled || current.error.isNotEmpty()) current.file.delete()
            current.level = 0
            current.finished = true
        }
    }
}
