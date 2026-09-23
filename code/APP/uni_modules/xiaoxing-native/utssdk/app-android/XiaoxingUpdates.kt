package uts.sdk.modules.xiaoxingNative

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.provider.OpenableColumns
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

object XiaoxingUpdates {
    private val busy=AtomicBoolean(false)
    private fun info(activity:Activity):JSONObject {
        val pkg=activity.packageManager.getPackageInfo(activity.packageName,0)
        val code=if(Build.VERSION.SDK_INT>=28) pkg.longVersionCode else pkg.versionCode.toLong()
        val preferences=activity.getSharedPreferences("xiaoxing-updates",Context.MODE_PRIVATE)
        val pending=preferences.getLong("pendingVersionCode",0)
        if(pending>0 && code>=pending) preferences.edit().remove("pendingVersionCode").apply()
        return JSONObject().put("versionCode",code).put("versionName",pkg.versionName).put("supported",true)
            .put("pendingVersionCode",if(pending>code) pending else 0)
    }
    fun call(activity:Activity,action:String,input:String,callback:(String)->Unit) {
        try {
            if(action=="update.info"){callback(info(activity).toString());return}
            if(action=="update.install") {
                if(Build.VERSION.SDK_INT>=26 && !activity.packageManager.canRequestPackageInstalls()) {
                    activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.packageName)))
                    callback(JSONObject().put("permissionRequired",true).toString());return
                }
                if(!busy.compareAndSet(false,true)){callback("{\"error\":\"正在准备安装，请稍候\"}");return}
                Thread {
                    var connection:HttpURLConnection?=null
                    var target:File?=null
                    try {
                        val args=JSONObject(input)
                        val url=URL(args.getString("url"))
                        require(url.protocol=="https" && url.host in setOf("www.chentong.xyz","47.100.172.149") && url.path.matches(Regex("/api/app/reminder/releases/[0-9]+/apk"))) {"安装包地址不受信任"}
                        val expected=args.getString("sha256")
                        require(expected.matches(Regex("[a-f0-9]{64}"))){"缺少安装包校验信息"}
                        val requestedCode=args.getLong("versionCode")
                        require(requestedCode>info(activity).getLong("versionCode")){"当前已安装此版本，请重新检查更新"}
                        // Never reuse a URI or overwrite a file already handed to an OEM installer.
                        val apk=XiaoxingUpdateFiles.create(activity.cacheDir,requestedCode,expected)
                        target=apk
                        connection=url.openConnection() as HttpURLConnection
                        connection!!.useCaches=false
                        connection!!.connectTimeout=15000;connection!!.readTimeout=30000;connection!!.instanceFollowRedirects=false
                        val headers=args.getJSONObject("headers")
                        for(key in headers.keys()) connection!!.setRequestProperty(key,headers.getString(key))
                        require(connection!!.responseCode==200){"下载失败，请检查网络或重新登录后重试"}
                        val digest=MessageDigest.getInstance("SHA-256")
                        var total=0L
                        connection!!.inputStream.use { source -> apk.outputStream().use { sink ->
                            val buffer=ByteArray(65536)
                            while(true){val count=source.read(buffer);if(count<0)break;total+=count;require(total<=300L*1024*1024){"安装包过大"};digest.update(buffer,0,count);sink.write(buffer,0,count)}
                        } }
                        require(total==args.getLong("fileSize")){"安装包下载不完整，请重试"}
                        val actual=digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
                        require(actual==expected){"安装包校验失败，请重试"}
                        val flags=if(Build.VERSION.SDK_INT>=28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
                        val candidate=activity.packageManager.getPackageArchiveInfo(apk.path,flags) ?: error("无法读取APK，请重新上传完整安装包")
                        val installed=activity.packageManager.getPackageInfo(activity.packageName,flags)
                        require(candidate.packageName==activity.packageName){"安装包包名不匹配"}
                        val code=if(Build.VERSION.SDK_INT>=28) candidate.longVersionCode else candidate.versionCode.toLong()
                        require(code==args.getLong("versionCode") && code>info(activity).getLong("versionCode")){"APK版本与发布信息不一致，或不是更新版本"}
                        val oldSignatures=if(Build.VERSION.SDK_INT>=28) installed.signingInfo!!.apkContentsSigners else installed.signatures
                        val newSignatures=if(Build.VERSION.SDK_INT>=28) candidate.signingInfo!!.apkContentsSigners else candidate.signatures
                        require(!oldSignatures.isNullOrEmpty() && !newSignatures.isNullOrEmpty() && oldSignatures.toSet()==newSignatures.toSet()){ "安装包签名不同，请使用原签名证书打包" }
                        activity.runOnUiThread {
                            try {
                                val uri=Uri.parse("content://"+activity.packageName+".xiaoxingupdates/"+apk.name)
                                val intent=Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.clipData=ClipData.newRawUri("APK",uri)
                                activity.startActivity(intent)
                                activity.getSharedPreferences("xiaoxing-updates",Context.MODE_PRIVATE).edit().putLong("pendingVersionCode",code).apply()
                                callback("{\"installerOpened\":true}")
                            }catch(e:Exception){callback(JSONObject().put("error","无法打开安装器："+e.message).toString())}
                            finally{busy.set(false)}
                        }
                    }catch(e:Exception){target?.delete();busy.set(false);activity.runOnUiThread{callback(JSONObject().put("error",e.message?:"更新失败，请重试").toString())}}
                    finally{connection?.disconnect()}
                }.start()
                return
            }
            callback("{\"supported\":false}")
        }catch(e:Exception){callback(JSONObject().put("error",e.message?:"无法检查更新").toString())}
    }
}

// Exposes only the verified APK in app-private cache, through a temporary URI grant.
class XiaoxingUpdateProvider:ContentProvider() {
    override fun onCreate()=true
    override fun getType(uri:Uri)="application/vnd.android.package-archive"
    private fun resolve(uri:Uri):File {
        return XiaoxingUpdateFiles.resolve(context!!.cacheDir,uri.path ?: "")
    }
    override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor {
        require(mode=="r")
        return ParcelFileDescriptor.open(resolve(uri),ParcelFileDescriptor.MODE_READ_ONLY)
    }
    override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor {
        val file=resolve(uri)
        val columns=(projection ?: arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE))
            .filter { it==OpenableColumns.DISPLAY_NAME || it==OpenableColumns.SIZE }.toTypedArray()
        return MatrixCursor(columns,1).apply {
            addRow(columns.map { if(it==OpenableColumns.DISPLAY_NAME) file.name else file.length() }.toTypedArray())
        }
    }
    override fun insert(uri:Uri,values:ContentValues?):Uri?=throw UnsupportedOperationException()
    override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?):Int=throw UnsupportedOperationException()
    override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?):Int=throw UnsupportedOperationException()
}
