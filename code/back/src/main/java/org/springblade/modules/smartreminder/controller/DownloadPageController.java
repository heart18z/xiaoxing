package org.springblade.modules.smartreminder.controller;

import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.service.DownloadPageService;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DownloadPageController {
    private final DownloadPageService page;
    @PostMapping("blade-smart/admin/releases/download-page")
    public R<Object> settings() {AppRoleGuard.requireAdmin();return R.data(page.settings());}
    @PostMapping("blade-smart/admin/releases/download-page/save")
    public R<Object> save(@RequestBody DownloadPageService.Settings input) {
        AppRoleGuard.requireAdmin();page.save(input);return R.success("下载配置已保存");
    }
    @GetMapping("app/download/config")
    public ResponseEntity<R<Object>> config() {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(R.data(page.publicConfig()));
    }
    @GetMapping("app/download/qr")
    public ResponseEntity<byte[]> qr() {
        return ResponseEntity.ok().header("Content-Type","image/png").header("Cache-Control","no-store").body(page.qr());
    }
    @GetMapping("app/download/android.apk")
    public ResponseEntity<FileSystemResource> apk() {
        var file=page.publicApk();
        return ResponseEntity.ok().header("Content-Type","application/vnd.android.package-archive")
            .header("Content-Disposition","attachment; filename=\"xiaoxing.apk\"")
            .header("Cache-Control","no-store").contentLength(file.length()).body(new FileSystemResource(file));
    }
}
