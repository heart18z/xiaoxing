package org.springblade.modules.smartreminder.controller;

import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.service.AppReleaseService;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class AppReleaseController {
    private final AppReleaseService releases;
    @PostMapping("blade-smart/admin/releases")
    public R<Object> list() { AppRoleGuard.requireAdmin(); return R.data(releases.list()); }
    @PostMapping("blade-smart/admin/releases/upload")
    public R<Object> upload(@RequestParam MultipartFile file, @RequestParam int versionCode,
            @RequestParam String versionName, @RequestParam String notes) throws Exception {
        AppRoleGuard.requireAdmin();
        return R.data(releases.upload(file, versionCode, versionName, notes));
    }
    @PostMapping("blade-smart/admin/releases/publish")
    public R<Object> publish(@RequestParam long id, @RequestParam boolean published) {
        AppRoleGuard.requireAdmin(); releases.publish(id, published); return R.success("操作成功");
    }
    @PostMapping("app/reminder/releases/latest")
    public R<Object> latest() { AppRoleGuard.requireAppUser(); return R.data(releases.latest()); }
    @GetMapping("app/reminder/releases/{id}/apk")
    public ResponseEntity<FileSystemResource> download(@PathVariable long id) {
        AppRoleGuard.requireAppUser();
        var file = releases.download(id);
        return ResponseEntity.ok().header("Content-Type", "application/vnd.android.package-archive")
            .header("Content-Disposition", "attachment; filename=\"xiaoxing-" + id + ".apk\"")
            .header("Cache-Control", "private, no-store").contentLength(file.length())
            .body(new FileSystemResource(file));
    }
}
