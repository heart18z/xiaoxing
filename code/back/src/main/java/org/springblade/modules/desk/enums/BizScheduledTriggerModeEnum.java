package org.springblade.modules.desk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizScheduledTriggerModeEnum {

    TASK_SYNC_AUTO("自动","auto"),
    TASK_SYNC_HAND("手动","hand");

    final String name;
    final String type;
}
