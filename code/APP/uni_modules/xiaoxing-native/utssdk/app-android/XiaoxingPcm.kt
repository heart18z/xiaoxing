package uts.sdk.modules.xiaoxingNative
import java.io.RandomAccessFile
import kotlin.math.log10
import kotlin.math.sqrt
object XiaoxingPcm {
    fun level(samples: ShortArray, count: Int): Int {
        if (count <= 0) return 0
        var energy = 0.0
        for (i in 0 until count) energy += samples[i].toDouble() * samples[i]
        val rms = sqrt(energy / count) / 32768.0
        return (((20 * log10(maxOf(rms, 0.000001)) + 60) / 60) * 100).toInt().coerceIn(0, 100)
    }
    fun header(out: RandomAccessFile, total: Int) {
        out.seek(0)
        fun le(value: Int, length: Int) { for (i in 0 until length) out.write(value ushr (i * 8) and 255) }
        out.writeBytes("RIFF"); le(total + 36, 4); out.writeBytes("WAVEfmt ")
        le(16, 4); le(1, 2); le(1, 2); le(16000, 4); le(32000, 4); le(2, 2); le(16, 2)
        out.writeBytes("data"); le(total, 4)
    }
}
