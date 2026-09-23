import uts.sdk.modules.xiaoxingNative.XiaoxingPcm
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
fun main() {
    check(XiaoxingPcm.level(ShortArray(1600),1600)==0)
    val quiet=XiaoxingPcm.level(ShortArray(1600){100},1600)
    val speech=XiaoxingPcm.level(ShortArray(1600){3000},1600)
    val loud=XiaoxingPcm.level(ShortArray(1600){30000},1600)
    check(quiet>0 && speech>quiet && loud>speech && loud<=100)
    check(XiaoxingPcm.level(shortArrayOf(-32768),1)==100)
    val file=File.createTempFile("pcm-regression", ".wav")
    try {
        RandomAccessFile(file,"rw").use { out -> out.write(ByteArray(44));out.write(ByteArray(3200){42});XiaoxingPcm.header(out,3200) }
        val bytes=file.readBytes();val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        check(bytes.size==3244 && String(bytes,0,4)=="RIFF" && String(bytes,8,8)=="WAVEfmt ")
        check(b.getInt(4)==3236 && b.getShort(20).toInt()==1 && b.getShort(22).toInt()==1)
        check(b.getInt(24)==16000 && b.getInt(28)==32000 && b.getShort(34).toInt()==16 && b.getInt(40)==3200)
        check(bytes[44].toInt()==42 && bytes.last().toInt()==42)
        println("PASS silence, speech/loudness ordering, clipping bounds and genuine mono 16kHz PCM WAV header/data")
    } finally { file.delete() }
}
