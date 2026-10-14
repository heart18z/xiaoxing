package org.springblade.modules.smartreminder.service;

import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.io.File;
import java.net.URI;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DownloadPageService {
    private final JdbcTemplate jdbc;
    private final AppReleaseService releases;
    @Value("${smart-reminder.download-page-url:https://47.100.172.149/download/}")
    private String defaultPageUrl;

    public record Settings(String pageUrl, boolean androidEnabled, boolean iosEnabled,
        String iosMode, String appStoreUrl, String testflightUrl, boolean harmonyApkEnabled,
        boolean harmonyNativeEnabled, String harmonyStoreUrl) {}

    public Settings settings() {
        var rows = jdbc.queryForList("select * from blade_app_download_page where id=1");
        if(rows.isEmpty()) return new Settings(defaultPageUrl, true, false, "testflight", "", "", true, false, "");
        var r=rows.get(0);
        return new Settings(text(r,"page_url"),flag(r,"android_enabled"),flag(r,"ios_enabled"),
            text(r,"ios_mode"),text(r,"app_store_url"),text(r,"testflight_url"),
            flag(r,"harmony_apk_enabled"),flag(r,"harmony_native_enabled"),text(r,"harmony_store_url"));
    }

    public void save(Settings input) {
        if(input==null) throw new ServiceException("请填写下载配置");
        String page=link(input.pageUrl(),"统一下载页网址",true,null);
        if(!Set.of("appstore","testflight").contains(Objects.toString(input.iosMode(),"")))
            throw new ServiceException("请选择 App Store 或 TestFlight 下载方式");
        String store=link(input.appStoreUrl(),"App Store 链接",false,Set.of("apps.apple.com"));
        String flight=link(input.testflightUrl(),"TestFlight 邀请链接",false,Set.of("testflight.apple.com"));
        if(!flight.isEmpty()&&!URI.create(flight).getPath().matches("/join/[A-Za-z0-9]+/?"))
            throw new ServiceException("请填写 TestFlight 公开邀请链接，例如 https://testflight.apple.com/join/邀请码");
        String harmony=link(input.harmonyStoreUrl(),"鸿蒙应用市场链接",false,
            Set.of("appgallery.huawei.com","appgallery.cloud.huawei.com"));
        if(input.iosEnabled() && ("appstore".equals(input.iosMode())?store:flight).isEmpty())
            throw new ServiceException("开启 iOS 下载前，请填写对应的下载链接");
        if(input.harmonyNativeEnabled()&&harmony.isEmpty())
            throw new ServiceException("开启 HarmonyOS 5 及以上下载前，请填写应用市场链接");
        jdbc.update("insert into blade_app_download_page(id,page_url,android_enabled,ios_enabled,ios_mode,app_store_url,testflight_url,harmony_apk_enabled,harmony_native_enabled,harmony_store_url) values(1,?,?,?,?,?,?,?,?,?) on duplicate key update page_url=values(page_url),android_enabled=values(android_enabled),ios_enabled=values(ios_enabled),ios_mode=values(ios_mode),app_store_url=values(app_store_url),testflight_url=values(testflight_url),harmony_apk_enabled=values(harmony_apk_enabled),harmony_native_enabled=values(harmony_native_enabled),harmony_store_url=values(harmony_store_url)",
            page,input.androidEnabled()?1:0,input.iosEnabled()?1:0,input.iosMode(),store,flight,
            input.harmonyApkEnabled()?1:0,input.harmonyNativeEnabled()?1:0,harmony);
    }

    public Map<String,Object> publicConfig() {
        var s=settings(); var latest=releases.latest();
        Map<String,Object> version=new LinkedHashMap<>();
        for(String key:List.of("id","versionCode","versionName","notes","fileSize","sha256"))
            if(latest.containsKey(key)) version.put(key,latest.get(key));
        return Map.of("settings",s,"android",version,"apkPath","/api/app/download/android.apk",
            "qrPath","/api/app/download/qr","androidAvailable",!latest.isEmpty());
    }

    public File publicApk() {
        var s=settings();
        if(!s.androidEnabled()&&!s.harmonyApkEnabled()) throw new ServiceException("安卓兼容版本正在适配中");
        var latest=releases.latest();
        if(latest.isEmpty()) throw new ServiceException("安卓版本暂未发布，请稍后再试");
        // Resolve the currently published file on every request: withdrawn/draft releases stay private.
        return releases.download(Long.parseLong(latest.get("id").toString()));
    }

    public byte[] qr() {
        return cn.hutool.extra.qrcode.QrCodeUtil.generatePng(settings().pageUrl(),600,600);
    }

    private static String text(Map<String,Object> row,String key) {return Objects.toString(row.get(key),"");}
    private static boolean flag(Map<String,Object> row,String key) {
        Object value=row.get(key);return Boolean.TRUE.equals(value)||value instanceof Number n&&n.intValue()==1;
    }
    private static String link(String value,String label,boolean required,Set<String> hosts) {
        String text=Objects.toString(value,"").trim();
        if(text.isEmpty()&&!required)return "";
        try {
            URI uri=URI.create(text);
            if(text.length()>1000||!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null
                ||uri.getUserInfo()!=null||uri.getPort()!=-1&&uri.getPort()!=443
                ||hosts!=null&&!hosts.contains(uri.getHost().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException();
            return text;
        } catch(IllegalArgumentException e) {throw new ServiceException(label+"必须是有效的 HTTPS "+(hosts==null?"网址":"官方链接"));}
    }
}
