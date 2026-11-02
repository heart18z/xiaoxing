package org.springblade.modules.smartreminder.controller;

import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springblade.modules.smartreminder.service.EventReportService;
import org.springblade.modules.smartreminder.service.ReportDocuments;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class EventReportController {
    private final EventReportService reports;
    private final ReportDocuments documents;
    @PostMapping("/app/reminder/reports/detail") public R<Object> detail(@RequestParam long id){AppRoleGuard.requireAppUser();return R.data(reports.detail(id,AuthUtil.getUserId(),false));}
    @GetMapping("/app/reminder/reports/download") public ResponseEntity<byte[]> download(@RequestParam long id,@RequestParam(defaultValue="pdf") String format)throws Exception{
        AppRoleGuard.requireAppUser();return file(reports.detail(id,AuthUtil.getUserId(),false),format);
    }
    @PostMapping("/blade-smart/admin/reports/list") public R<Object> list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="")String keyword,@RequestParam(defaultValue="")String start,@RequestParam(defaultValue="")String status){AppRoleGuard.requireAdmin();return R.data(reports.list(page,keyword,start,status));}
    @PostMapping("/blade-smart/admin/reports/users") public R<Object> users(@RequestParam(defaultValue="")String keyword){AppRoleGuard.requireAdmin();return R.data(reports.users(keyword));}
    @PostMapping("/blade-smart/admin/reports/settings") public R<Object> settings(@RequestParam(defaultValue="0")long userId){AppRoleGuard.requireAdmin();return R.data(reports.settings(userId));}
    @PostMapping("/blade-smart/admin/reports/settings/save") public R<Object> save(@RequestBody Map<String,Object> input){AppRoleGuard.requireAdmin();reports.saveSettings(Long.parseLong(input.getOrDefault("userId",0).toString()),Boolean.TRUE.equals(input.get("enabled")),Integer.parseInt(input.getOrDefault("sendDay",1).toString()),Integer.parseInt(input.getOrDefault("sendHour",9).toString()));return R.data(true);}
    @PostMapping("/blade-smart/admin/reports/generate") public R<Object> generate(@RequestBody Map<String,String> input){
        AppRoleGuard.requireAdmin();long user=Long.parseLong(input.get("userId"));String request=input.getOrDefault("requestId","");
        if(!request.matches("[A-Za-z0-9_-]{16,80}"))throw new IllegalArgumentException("请求编号无效");
        return R.data(reports.generate(user,EventReportService.period(input.get("start"),input.get("end")),"MANUAL","MANUAL:"+user+":"+request));
    }
    @PostMapping("/blade-smart/admin/reports/send") public R<Object> send(@RequestBody Map<String,String> input){AppRoleGuard.requireAdmin();reports.send(Long.parseLong(input.get("id")),AuthUtil.getUserId(),input.getOrDefault("requestId","INITIAL"));return R.data(true);}
    @PostMapping("/blade-smart/admin/reports/detail") public R<Object> adminDetail(@RequestParam long id){AppRoleGuard.requireAdmin();return R.data(reports.detail(id,AuthUtil.getUserId(),true));}
    @GetMapping("/blade-smart/admin/reports/download") public ResponseEntity<byte[]> adminDownload(@RequestParam long id,@RequestParam(defaultValue="pdf")String format)throws Exception{AppRoleGuard.requireAdmin();return file(reports.detail(id,AuthUtil.getUserId(),true),format);}
    private ResponseEntity<byte[]> file(Map<String,Object> report,String format)throws Exception{
        if(!"READY".equals(report.get("status")))throw new IllegalArgumentException("报告尚未生成完成");
        if(!List.of("pdf","docx").contains(format))throw new IllegalArgumentException("格式无效");
        String name="小醒事件报告_"+report.get("start")+"_"+report.get("end")+"."+format;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.parseMediaType(format.equals("pdf")?"application/pdf":"application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(documents.export(report,format));
    }
}
