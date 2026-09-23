import uts.sdk.modules.xiaoxingNative.XiaoxingUpdateFiles
import java.nio.file.Files

fun main() {
    val directory=Files.createTempDirectory("xiaoxing-update-files-").toFile()
    fun rejects(name:String,block:()->Unit) {
        var rejected=false
        try { block() } catch(e:IllegalArgumentException) { rejected=true }
        check(rejected) { name };println("PASS $name")
    }
    try {
        val old=XiaoxingUpdateFiles.create(directory,118,"a".repeat(64));old.writeText("old APK")
        val fresh=XiaoxingUpdateFiles.create(directory,125,"b".repeat(64));fresh.writeText("new APK")
        val retry=XiaoxingUpdateFiles.create(directory,125,"b".repeat(64));retry.writeText("retried APK")
        check(setOf(old.name,fresh.name,retry.name).size==3);println("PASS upgrade and retry have distinct installer URIs")
        check(XiaoxingUpdateFiles.resolve(directory,"/"+old.name).readText()=="old APK")
        check(XiaoxingUpdateFiles.resolve(directory,"/"+fresh.name).readText()=="new APK")
        check(XiaoxingUpdateFiles.resolve(directory,"/"+retry.name).readText()=="retried APK")
        println("PASS outstanding installer URIs retain their own APK content")
        rejects("old shared URI rejected") { XiaoxingUpdateFiles.resolve(directory,"/update.apk") }
        rejects("path traversal rejected") { XiaoxingUpdateFiles.resolve(directory,"/../"+fresh.name) }
        rejects("nested path rejected") { XiaoxingUpdateFiles.resolve(directory,"/other/"+fresh.name) }
        rejects("missing APK rejected") { XiaoxingUpdateFiles.resolve(directory,"/"+XiaoxingUpdateFiles.create(directory,126,"c".repeat(64)).name) }
        rejects("invalid release metadata rejected") { XiaoxingUpdateFiles.create(directory,0,"invalid") }
    } finally { directory.deleteRecursively() }
}
