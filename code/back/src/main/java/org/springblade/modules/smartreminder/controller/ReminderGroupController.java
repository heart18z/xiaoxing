package org.springblade.modules.smartreminder.controller;

import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.dto.ReminderGroupDtos;
import org.springblade.modules.smartreminder.service.ReminderGroupService;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("app/reminder/groups")
public class ReminderGroupController {
    private final ReminderGroupService groups;
    private Long user(){AppRoleGuard.requireAppUser();return AuthUtil.getUserId();}
    @PostMapping("/list") public R<Object> list(){return R.data(groups.list(user()));}
    @PostMapping("/detail") public R<Object> detail(@RequestParam Long groupId){return R.data(groups.detail(user(),groupId));}
    @PostMapping("/create") public R<Object> create(@RequestBody ReminderGroupDtos.Save input){return R.data(groups.create(user(),input));}
    @PostMapping("/save") public R<Object> save(@RequestBody ReminderGroupDtos.Save input){return R.data(groups.save(user(),input));}
    @PostMapping("/invite") public R<Object> invite(@RequestBody ReminderGroupDtos.Members input){return R.data(groups.invite(user(),input));}
    @PostMapping("/role") public R<Object> role(@RequestBody ReminderGroupDtos.Member input){return R.data(groups.role(user(),input));}
    @PostMapping("/admins") public R<Object> admins(@RequestBody ReminderGroupDtos.Members input){return R.data(groups.admins(user(),input));}
    @PostMapping("/member/tags") public R<Object> memberTags(@RequestBody ReminderGroupDtos.Member input){return R.data(groups.memberTags(user(),input));}
    @PostMapping("/tags/save") public R<Object> tag(@RequestBody ReminderGroupDtos.Tag input){return R.data(groups.saveTag(user(),input));}
    @PostMapping("/tags/delete") public R<Object> deleteTag(@RequestBody ReminderGroupDtos.Tag input){return R.data(groups.deleteTag(user(),input));}
    @PostMapping("/member/remove") public R<Object> remove(@RequestBody ReminderGroupDtos.Member input){groups.remove(user(),input);return R.data(true);}
    @PostMapping("/dissolve") public R<Object> dissolve(@RequestBody ReminderGroupDtos.Save input){groups.dissolve(user(),input.getGroupId());return R.data(true);}
}
