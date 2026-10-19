package org.springblade.modules.smartreminder.dto;

import lombok.Data;
import java.util.List;

public final class ReminderGroupDtos {
    private ReminderGroupDtos() {}
    @Data public static class Save {
        private Long groupId;
        private String name;
        private String avatar;
        private String introduction;
        private String groupType = "CUSTOM";
    }
    @Data public static class Members {
        private Long groupId;
        private List<Long> userIds = List.of();
    }
    @Data public static class Member {
        private Long groupId;
        private Long userId;
        private String role;
        private List<String> tags = List.of();
    }
    @Data public static class Tag {
        private Long groupId;
        private Long tagId;
        private String name;
        private List<Long> userIds = List.of();
    }
    @Data public static class CandidatePeople {
        private Long candidateId;
        private Integer eventIndex = 0;
        private List<Long> userIds = List.of();
    }
}
