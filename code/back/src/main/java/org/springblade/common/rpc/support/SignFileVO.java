package org.springblade.common.rpc.support;

import lombok.Data;

@Data
public class SignFileVO {
    private Long id;
    private String fileName;
    private String link;
    /**
     * 原版文件id
     */
    private Long originalId;
    private String signStatus;

}

