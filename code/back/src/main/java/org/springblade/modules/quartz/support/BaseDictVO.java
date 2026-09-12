package org.springblade.modules.quartz.support;

import lombok.Data;

@Data
public class BaseDictVO {
	private String partByDate; //更新日期
	private String fzdzdm; //父字典值代码
	private Integer zdzcj; //字典值层级
	private String zdzbz; //字典值备注
	private String zdzkfxz; //字典值开放性质
	private String zdzmc; //字典值名称
	private String zdzdm; //字典值代码
	private Long fjzj; //父级主键
	private Long zdzj; //字典主键
	private Integer zdkfzt; //字典开放状态
	private String zdbm; //字典编码
	private String zdmc; //字典名称
	private Integer cjxz; //层级限制
	private String bjgdm;
	private String xtmc;
	private String zdly;
}
