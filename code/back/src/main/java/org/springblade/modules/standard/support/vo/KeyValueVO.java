package org.springblade.modules.standard.support.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "键值对")
public class KeyValueVO {

	@Schema(description = "键")
	private String key;

	@Schema(description = "值")
	private String value;
}
