import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.config.DownloadPageSchema;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.imageio.ImageIO;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DownloadPageRegression {
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);System.out.println("PASS "+name);}
    static void rejected(Runnable action,String name){try{action.run();}catch(org.springblade.core.log.exception.ServiceException expected){check(true,name);return;}throw new AssertionError(name);}
    static DownloadPageService.Settings settings(boolean android,boolean ios,String mode,String store,String flight,boolean apk,boolean nativeHarmony,String harmony){return new DownloadPageService.Settings("https://47.100.172.149/download/",android,ios,mode,store,flight,apk,nativeHarmony,harmony,null);}
    public static void main(String[] args)throws Exception {
        // MySQL CHAR casts infer the ID length; H2 CHAR defaults to a single character.
        var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:download;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","")) {
            @Override public List<Map<String,Object>> queryForList(String sql) {
                return super.queryForList(sql.replace("cast(id as char)","cast(id as varchar)"));
            }
        };
        db.execute("create table blade_app_release(id bigint primary key,version_code int,version_name varchar(40),notes varchar(4000),file_name varchar(255),file_size bigint,sha256 varchar(64),published int,create_time timestamp)");
        var schema=new DownloadPageSchema(db);schema.migrate();schema.migrate();
        var release=new AppReleaseService(db);var service=new DownloadPageService(db,release);
        var f=DownloadPageService.class.getDeclaredField("defaultPageUrl");f.setAccessible(true);f.set(service,"https://47.100.172.149/download/");
        var directory=Files.createTempDirectory("download-page-test");
        f=AppReleaseService.class.getDeclaredField("uploadDir");f.setAccessible(true);f.set(release,directory.toString());
        try {
            var defaults=service.settings();
            check(defaults.androidEnabled()&&defaults.harmonyApkEnabled()&&!defaults.iosEnabled()&&!defaults.harmonyNativeEnabled(),"safe default platform switches");
            check(!Boolean.TRUE.equals(service.publicConfig().get("androidAvailable")),"no APK published is represented clearly");
            rejected(()->service.publicApk(),"no release cannot download");
            service.save(settings(true,true,"testflight","","https://testflight.apple.com/join/AbCd1234",true,false,""));
            check(service.settings().iosEnabled()&&service.settings().iosMode().equals("testflight"),"TestFlight settings persisted");
            rejected(()->service.save(settings(true,true,"appstore","","",true,false,"")),"enabled iOS requires target link");
            rejected(()->service.save(settings(true,false,"testflight","","https://testflight.apple.com/not-an-invite",true,false,"")),"TestFlight requires public invite URL");
            rejected(()->service.save(settings(true,false,"appstore","https://apps.apple.com.evil.test/app/id1","",true,false,"")),"spoofed Apple host rejected");
            rejected(()->service.save(settings(true,true,"appstore","javascript:alert(1)","",true,false,"")),"script link rejected");
            rejected(()->service.save(settings(true,false,"other","","",true,false,"")),"unsupported iOS mode rejected");
            rejected(()->service.save(settings(true,false,"testflight","","",true,true,"")),"native HarmonyOS requires store link");
            rejected(()->service.save(new DownloadPageService.Settings("http://example.com",true,false,"testflight","","",true,false,"",null)),"insecure landing URL rejected");
            check(service.settings().testflightUrl().endsWith("AbCd1234"),"invalid saves do not overwrite configuration");
            String store="https://apps.apple.com/cn/app/id12345678", harmony="https://appgallery.huawei.com/app/C12345678";
            service.save(settings(true,true,"appstore",store,"https://testflight.apple.com/join/AbCd1234",true,true,harmony));
            var fresh=new DownloadPageService(db,release);
            check(fresh.settings().iosMode().equals("appstore")&&fresh.settings().testflightUrl().endsWith("AbCd1234")&&fresh.settings().harmonyNativeEnabled(),"switch mode retains alternate link and survives new service");
            db.execute("alter table blade_app_download_page drop column harmony_enabled");
            schema.migrate();schema.migrate();
            check(service.settings().harmonyEnabled()&&service.settings().harmonyStoreUrl().equals(harmony),"existing configuration migrates idempotently without losing links");
            Files.createDirectories(directory.resolve("app-releases"));
            long oldId=930000000000000001L,newId=930000000000000002L,draftId=930000000000000003L;
            for(long id:List.of(oldId,newId,draftId))Files.writeString(directory.resolve("app-releases").resolve(id+".apk"),"apk:"+id);
            db.update("insert into blade_app_release values(?,125,'1.0.125','old','old.apk',24,'oldhash',1,now())",oldId);
            db.update("insert into blade_app_release values(?,134,'1.0.134','new','new.apk',25,'newhash',1,now())",newId);
            db.update("insert into blade_app_release values(?,999,'draft','secret','draft.apk',26,'drafthash',0,now())",draftId);
            check(service.publicApk().getName().equals(newId+".apk"),"public APK resolves newest published artifact");
            var info=(Map<?,?>)service.publicConfig().get("android");
            check(info.get("id").toString().equals(Long.toString(newId))&&!info.containsKey("fileName")&&!info.containsKey("createUser"),"public metadata excludes draft and internal filename, preserves exact ID");
            var image=ImageIO.read(new ByteArrayInputStream(service.qr()));
            int width=image.getWidth(),height=image.getHeight();
            var pixels=image.getRGB(0,0,width,height,null,0,width);
            var result=new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width,height,pixels))));
            check(result.getText().equals(service.settings().pageUrl()),"QR decodes to canonical stable landing URL");
            release.publish(newId,false);
            check(service.publicApk().getName().equals(oldId+".apk"),"withdraw immediately falls back to remaining published APK");
            service.save(settings(false,false,"testflight","","",false,false,""));
            rejected(()->service.publicApk(),"all APK switches off blocks public download");
            service.save(settings(false,false,"testflight","","",true,false,""));
            check(service.publicApk().exists(),"legacy HarmonyOS can download independently of Android switch");
            service.save(new DownloadPageService.Settings(defaults.pageUrl(),false,false,"testflight","","",true,true,harmony,false));
            check(!service.settings().harmonyEnabled()&&service.settings().harmonyApkEnabled()&&service.settings().harmonyNativeEnabled()&&service.settings().harmonyStoreUrl().equals(harmony),"master switch persists without erasing HarmonyOS choices or URL");
            rejected(()->service.publicApk(),"closed HarmonyOS master blocks APK even when compatibility choice is retained");
            service.save(settings(false,false,"testflight","","",true,true,harmony));
            check(!service.settings().harmonyEnabled(),"older admin save missing new field preserves closed channel");
            service.save(new DownloadPageService.Settings(defaults.pageUrl(),true,false,"testflight","","",true,true,harmony,false));
            check(service.publicApk().exists(),"Android stays independent of closed HarmonyOS channel");
            service.save(new DownloadPageService.Settings(defaults.pageUrl(),false,false,"testflight","","",true,true,harmony,true));
            check(service.publicApk().exists(),"reopening master restores compatible download");
            release.publish(oldId,false);
            rejected(()->service.publicApk(),"all releases withdrawn cannot download");
        } finally {try(var paths=Files.walk(directory)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }
}
