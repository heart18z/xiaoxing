package org.springblade.common.rpc.support;

import lombok.Data;
import org.springblade.modules.resource.vo.AttachVO;

import java.util.List;

@Data
public class MessageSystemData {

	private MessageSystemNotice notice;

	private List<String> users;

	private List<AttachVO> attachList;

	private String systemId;

	private String systemName;

	private String sendUserAccount;
}
