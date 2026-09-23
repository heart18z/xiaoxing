package uts.sdk.modules.xiaoxingNative

import java.io.File
import java.util.UUID

// Each installation attempt owns an immutable path, including retries of the same version.
object XiaoxingUpdateFiles {
    fun create(cacheDir:File,code:Long,sha256:String):File {
        require(code>0 && sha256.matches(Regex("[a-f0-9]{64}")))
        val directory=File(cacheDir,"app-updates")
        require(directory.isDirectory || directory.mkdirs()){"无法创建安装包目录"}
        return File(directory,"xiaoxing-$code-$sha256-${UUID.randomUUID()}.apk")
    }
    fun resolve(cacheDir:File,path:String):File {
        require(path.matches(Regex("/xiaoxing-[1-9][0-9]*-[a-f0-9]{64}-[a-f0-9-]{36}\\.apk")))
        val directory=File(cacheDir,"app-updates").canonicalFile
        val file=File(directory,path.substring(1)).canonicalFile
        require(file.parentFile==directory && file.isFile){"安装包不存在，请重新下载"}
        return file
    }
}
