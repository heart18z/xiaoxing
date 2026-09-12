package org.springblade.modules.desk.vo;

import lombok.Data;

@Data
public class SyncPeopleVO {
    private String ryh; //人员号（人员唯一编号）
    private String xm; //姓名
    private String xb; //性别
    private String dzyj; //电子邮件
    private String sj; //手机号
    private String wxid; // 微信ID
    private String csrq; // 出生日期
    private Integer sfsc; // 是否删除 （1：是，0：否）
    private String statDate; // 更新时间
	private String sfzjh; // 身份证件号
}
