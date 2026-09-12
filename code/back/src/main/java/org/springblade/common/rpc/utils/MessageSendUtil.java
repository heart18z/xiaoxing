package org.springblade.common.rpc.utils;

import org.springblade.common.cache.ParamCache;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformHttp;
import org.springblade.common.rpc.apiplatform.util.ApiTokenUtil;
import org.springblade.common.rpc.support.MessageSystemData;
import org.springblade.common.rpc.support.MessageSystemNotice;
import org.springblade.core.tool.api.R;
import org.springblade.modules.resource.vo.AttachVO;

import java.util.ArrayList;
import java.util.List;

public class MessageSendUtil {

	/**
	 * 必要参数
	 * {
	 *     "attachList": [
	 *         {
	 *
	 *             "attachSize": 142707847,
	 *             "link": "https://minio.sinoaopt.com/b005/upload/20230923/028fd94e89d22060138a679b467e1616.mp4",
	 *             "originalName": "公司基本介绍.mp4"
	 *         },
	 *     ],
	 *     "notice": {
	 *         "category": "210",
	 *         "href": "http://localhost:1888/#/demo/standard/standard?fromId=1705422377178968066",
	 *         "noticeContent": "",
	 *         "noticeSentTimeRange": [
	 *             "2023-12-22 01:08:50",
	 *             "2023-12-22 01:08:50"
	 *         ],
	 *         "noticeTitle": "22",
	 *         "noticeType": "200",
	 *     },
	 *     "users": [
	 *         "admin"
	 *     ],
	 *     "systemId": "",
	 *     "systemName": "测试系统",
	 *     "sendUserAccount": "adm"
	 * }
	 * @return
	 */
	/**
	 * 发送消息通知工具 注意在发送之前需要检查《对外交互》功能内的 发送消息通知功能是否打开
	 * @param href 消息通知附带链接
	 * @param content 消息内容（可以是富文本）
	 * @param title	消息标题
	 * @param users 发送目标（接收人的账号）
	 * @param attachVOList 附件列表
	 * @return 是否成功
	 */
	public static Boolean doSendMessage(	String href, String content,
											String title,	List<String> users, List<AttachVO> attachVOList){
		//常规事务
		final String commonMessage = "210";
		final String commonType = "200";
		MessageSystemData messageSystemData =new MessageSystemData();
		MessageSystemNotice notice = new MessageSystemNotice(){{
			setCategory(commonMessage);
			setHref(href);
			setNoticeContent(content);
			setNoticeType(commonType);
			setNoticeTitle(title);
		}};
		messageSystemData.setSystemId(ParamCache.getValue("system.id"));
		messageSystemData.setSystemName(ParamCache.getValue("project.name"));
		messageSystemData.setNotice(notice);
		messageSystemData.setAttachList(attachVOList);
		messageSystemData.setUsers(users);

		R<Object> objectR = ApiPlatformHttp.commonPostByInteractive(messageSystemData,
			ApiPlatformUrlConstant.SEND_MESSAGE_INTERACTIVE_CODE,
			ApiPlatformUrlConstant.SEND_MESSAGE_URL);
		return true;
	}

	public static void sendMessageDemo() {
		List<AttachVO> attachVOList = new ArrayList<>();
		attachVOList.add(new AttachVO(){{
			setAttachSize(142707847L);
			setLink("https://minio.sinoaopt.com/b005/upload/20230923/028fd94e89d22060138a679b467e1616.mp4");
			setOriginalName("公司基本介绍.mp4");
		}});

		doSendMessage(
			"http://eastview.top:40028/#/system/dictbiz?menuId=1164733379658963251",
			"测试消息",

			"消息标题",
			new ArrayList<String>(){{add("2023062603");}},
			attachVOList
		);
	}

}
