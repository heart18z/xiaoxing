package org.springblade.modules.smartreminder.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
public class AppReleaseService {
    private final JdbcTemplate jdbc;
    @Value("${smart-reminder.upload-dir:${user.dir}/uploads/smart-reminder}")
    private String uploadDir;
    private static final String COLUMNS = "cast(id as char) as id,version_code as versionCode,version_name as versionName,notes,coalesce(file_name,concat(id,'.apk')) as fileName,file_size as fileSize,sha256,published,create_time as createTime";
    public List<Map<String,Object>> list() {
        return jdbc.queryForList("select " + COLUMNS + " from blade_app_release order by version_code desc limit 100");
    }
    public Map<String,Object> latest() {
        var rows = jdbc.queryForList("select " + COLUMNS + " from blade_app_release where published=1 order by version_code desc limit 1");
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }
    public Map<String,Object> upload(MultipartFile file, int code, String name, String notes) throws Exception {
        if(code < 1 || name == null || name.isBlank() || name.length()>40 || notes == null || notes.isBlank() || notes.length()>4000)
            throw new ServiceException("请填写有效版本号和更新说明（最多4000字）");
        if(file.isEmpty() || file.getSize()>300L*1024*1024 || !Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT).endsWith(".apk"))
            throw new ServiceException("请选择不超过300MB的APK安装包");
        if(jdbc.queryForObject("select count(*) from blade_app_release where version_code=?",Integer.class,code)>0)
            throw new ServiceException("版本代码已存在，请先删除对应的草稿/已下架版本，或使用新的版本代码");
        String original=Objects.toString(file.getOriginalFilename(), "app.apk").replace('\\','/');
        String fileName=original.substring(original.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]", "");
        if(fileName.length()>255)throw new ServiceException("APK文件名最多255个字符");
        long id=IdWorker.getId();
        Path directory=Path.of(uploadDir,"app-releases").toAbsolutePath();
        Files.createDirectories(directory);
        Path target=directory.resolve(id+".apk");
        try {
            try(var input=file.getInputStream()) { Files.copy(input,target); }
            try(var zip=new ZipFile(target.toFile())) {
                if(zip.getEntry("AndroidManifest.xml")==null || zip.getEntry("classes.dex")==null)
                    throw new ServiceException("不是完整的Android APK安装包");
            }
            var digest=MessageDigest.getInstance("SHA-256");
            try(var input=Files.newInputStream(target)) {
                byte[] buffer=new byte[65536]; int count;
                while((count=input.read(buffer))!=-1) digest.update(buffer,0,count);
            }
            String hash=HexFormat.of().formatHex(digest.digest());
            jdbc.update("insert into blade_app_release(id,version_code,version_name,notes,file_name,file_size,sha256,published,create_user,create_time) values(?,?,?,?,?,?,?,0,?,now())",
                id,code,name.trim(),notes.trim(),fileName,file.getSize(),hash,AuthUtil.getUserId());
            return Map.of("id",String.valueOf(id),"sha256",hash);
        } catch(Exception e) {
            Files.deleteIfExists(target);
            if(e instanceof DuplicateKeyException)throw new ServiceException("版本代码已存在，请先删除对应的草稿/已下架版本，或使用新的版本代码");
            throw e;
        }
    }
    public void publish(long id, boolean published) {
        if(published) downloadFile(id);
        if(jdbc.update("update blade_app_release set published=? where id=?",published?1:0,id)!=1)
            throw new ServiceException("版本不存在");
    }
    public void delete(long id) {
        // Conditional delete is atomic with concurrent publish/withdraw operations.
        if(jdbc.update("delete from blade_app_release where id=? and published=0",id)!=1)
            throw new ServiceException("仅可删除草稿或已下架版本，请先下架；版本也可能已被删除");
        try { Files.deleteIfExists(Path.of(uploadDir,"app-releases",id+".apk")); }
        catch(java.io.IOException ignored) {
            // The deleted row immediately revokes download access; keep cleanup failures observable.
            org.slf4j.LoggerFactory.getLogger(AppReleaseService.class).warn("APK cleanup deferred for deleted release {}",id);
        }
    }
    private File downloadFile(long id) {
        File file=Path.of(uploadDir,"app-releases",id+".apk").toFile();
        if(!file.isFile()) throw new ServiceException("安装包不存在，请重新上传");
        return file;
    }
    public File download(long id) {
        if(jdbc.queryForObject("select count(*) from blade_app_release where id=? and published=1",Integer.class,id)!=1)
            throw new ServiceException("版本尚未发布或已下架，请重新检查更新");
        return downloadFile(id);
    }
}
