package uts.sdk.modules.xiaoxingNative

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class XiaoxingAndroid {
    companion object {
        private var navigationBottom: Double? = null
        @JvmStatic fun navigationInset(activity: Activity): Double {
            navigationBottom?.let { return it }
            val insets = activity.window.decorView.rootWindowInsets ?: return 0.0
            val pixels = if (android.os.Build.VERSION.SDK_INT >= 30)
                insets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.navigationBars()).bottom
            else insets.stableInsetBottom
            val value = pixels / activity.resources.displayMetrics.density.toDouble()
            navigationBottom = value
            return value
        }

        private const val ALIAS="xiaoxing.uniappx.secure.v1"
        private fun key():SecretKey {
            val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
            (store.getKey(ALIAS,null) as? SecretKey)?.let{return it}
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{
                init(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
        }
        @JvmStatic fun read(context:Context,name:String):String {
            val stored=context.getSharedPreferences(ALIAS,Context.MODE_PRIVATE).getString(name,null)?:return ""
            return try{val bytes=Base64.decode(stored,Base64.NO_WRAP);val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)));String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8)}catch(_:Exception){""}
        }
        @JvmStatic fun write(context:Context,name:String,value:String):Boolean {
            return try{val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());val bytes=cipher.iv+cipher.doFinal(value.toByteArray(Charsets.UTF_8));context.getSharedPreferences(ALIAS,Context.MODE_PRIVATE).edit().putString(name,Base64.encodeToString(bytes,Base64.NO_WRAP)).commit()}catch(_:Exception){false}
        }
        @JvmStatic fun random(count:Int):String {require(count in 1..1024);val bytes=ByteArray(count);SecureRandom().nextBytes(bytes);return bytes.joinToString(""){"%02x".format(it)}}
        @JvmStatic fun pick(activity:Activity,callback:(String)->Unit){
            activity.runOnUiThread {
                if(activity.fragmentManager.findFragmentByTag("xiaoxing.files")!=null){callback(JSONObject().put("error","文件选择正在进行").toString());return@runOnUiThread}
                val fragment=XiaoxingFilePicker();fragment.callback=callback
                activity.fragmentManager.beginTransaction().add(fragment,"xiaoxing.files").commitAllowingStateLoss()
            }
        }
    }
}

@Suppress("DEPRECATION")
class XiaoxingFilePicker:Fragment(){
    var callback:((String)->Unit)?=null
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);retainInstance=true
        if(savedInstanceState==null)try{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="*/*"},7241)}catch(_:Exception){finish(JSONObject().put("error","无法打开文件选择器"))}
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        if(requestCode!=7241)return
        val uri=data?.data;if(resultCode!=Activity.RESULT_OK||uri==null){finish(JSONObject().put("cancelled",true));return}
        try{
            val resolver=activity.contentResolver;var name="document";var size=0L
            resolver.query(uri,null,null,null,null)?.use{cursor->if(cursor.moveToFirst()){val ni=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);val si=cursor.getColumnIndex(OpenableColumns.SIZE);if(ni>=0)name=cursor.getString(ni);if(si>=0)size=cursor.getLong(si)}}
            if(size>20L*1024*1024){finish(JSONObject().put("error","文件不能超过20MB"));return}
            val file=File(activity.cacheDir,"xiaoxing-${System.nanoTime()}-${File(name).name}")
            resolver.openInputStream(uri)!!.use{input->file.outputStream().use{output->val buffer=ByteArray(8192);var total=0L;while(true){val n=input.read(buffer);if(n<0)break;total+=n;if(total>20L*1024*1024)throw IllegalArgumentException("文件过大");output.write(buffer,0,n)};size=total}}
            finish(JSONObject().put("path",file.absolutePath).put("name",name).put("size",size))
        }catch(_:Exception){finish(JSONObject().put("error","文件读取失败或超过20MB"))}
    }
    private fun finish(result:JSONObject){callback?.invoke(result.toString());callback=null;fragmentManager?.beginTransaction()?.remove(this)?.commitAllowingStateLoss()}
}
