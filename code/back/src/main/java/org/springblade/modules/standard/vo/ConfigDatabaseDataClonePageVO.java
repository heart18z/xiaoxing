package org.springblade.modules.standard.vo;

import cn.hutool.db.Entity;
import cn.hutool.db.PageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springblade.core.tool.support.Kv;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigDatabaseDataClonePageVO {

	private Long total;

	private List<Kv> fields;

	private PageResult<Entity> records;

	private List<String> props;
}
