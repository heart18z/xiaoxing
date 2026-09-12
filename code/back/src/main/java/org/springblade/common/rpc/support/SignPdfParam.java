package org.springblade.common.rpc.support;

import lombok.Data;

@Data
public class SignPdfParam {

	private String fileName;
	private String link;
	private String sourceSystemRowId;
	private String sourceSystemId;
	private String sourceSystemName;
	private String belongAccount;
}
