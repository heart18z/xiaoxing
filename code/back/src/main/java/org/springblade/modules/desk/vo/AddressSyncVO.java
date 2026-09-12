package org.springblade.modules.desk.vo;

import lombok.Data;

@Data
public class AddressSyncVO {

	private String wybh; //物业编号（唯一编号）
	private String wymc; //物业名称
	private String ssdq; //所属地区
	private String wytxdz; //物业通信地址
	private String wyfzr; //物业负责人
	private String jzwpmt; // 建筑物平面图
	private String bhqu; // 包含区间
	private Integer sfsc; // 是否删除 （1：是，0：否）
	private String statDate; // 更新时间

}
