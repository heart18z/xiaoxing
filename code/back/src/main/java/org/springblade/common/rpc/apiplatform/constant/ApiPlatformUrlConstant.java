package org.springblade.common.rpc.apiplatform.constant;

public interface ApiPlatformUrlConstant {
	String AUTH_TOKEN_URL = "api/getAccesstoken";

	String DICT_URL ="api/api/dict";

	String SYNC_ADDRESS_URL = "api/api/address/data";

	String SYNC_ADDRESS_INTERACTIVE_CODE = "address";


	String SYNC_PEOPLE_FULL_URL = "http://192.168.9.40:8520/api/system/userdata";

	String SYNC_PEOPLE_INTERACTIVE_CODE = "people";

	String SYNC_ROOM_URL = "api/api/prange/data";

	String POST_ATTACH_URL = "/api/api/files/files-put";

	String POST_ATTACH_INTERACTIVE_CODE = "files-put";

	String SYNC_FILES_URL ="api/api/files/files-list";

	String SYNC_FILES_INTERACTIVE_CODE ="files-list";

	String GET_BASE_DICT = "api/api/dict";

	String GET_BASE_INTERACTIVE_CODE = "dict";

	String SEND_MESSAGE_URL="api/api/messagse/send-message";

	String SEND_MESSAGE_INTERACTIVE_CODE="send-message";
	//创建文件签署
	String CREATE_SIGN_URL ="api/api/sign/create-sign";
	//获取文件签署
	String SIGN_LIST_URL ="api/api/sign/sign-list";

	String SIGN_LIST_INTERACTIVE_CODE ="sign-list";
}
