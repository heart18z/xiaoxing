package org.springblade.common.rpc.apiplatform.interceptor;

import com.alibaba.fastjson.JSON;
import okhttp3.*;
import okio.Buffer;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformConstant;
import org.springblade.common.rpc.apiplatform.util.ApiAuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class TokenRetryInterceptor implements Interceptor{
    public int maxRetryCount;
    private int count = 0;
    public TokenRetryInterceptor() {
        this.maxRetryCount = 1;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

		return retry(chain);

    }


	// 403重试
    public Response retry(Chain chain){

        Response response = null;
		// 添加token
		String token = ApiAuthUtil.getTokenString();
		Request originalRequest = chain.request();
		Request.Builder requestBuilder = originalRequest.newBuilder();
		HttpUrl.Builder urlBuilder = originalRequest.url().newBuilder();
		urlBuilder.addEncodedQueryParameter(ApiPlatformConstant.API_ACCESS_TOKEN_NAME,token);
		HttpUrl httpUrl = urlBuilder.build();
		requestBuilder.url(httpUrl);
		byte[] respBytes = null;

        try {
			// 解析返回值 如果code为401则重新获取token重新请求
			response =chain.proceed(requestBuilder.build());
            String responseBody = response.body().string();
			R resData = JSON.parseObject(responseBody,R.class);


			respBytes = resData.getData().toString().getBytes(StandardCharsets.UTF_8);
            while (resData.getCode()==ApiPlatformConstant.API_ACCESS_TOKEN_ERROR_CODE && count < maxRetryCount) {
				System.out.println("retry======>"+ApiPlatformConstant.API_ACCESS_TOKEN_ERROR_CODE);
                count++;
				ApiAuthUtil.removeAccessToken();
                return retry(chain);
            }
        }
        catch (Exception e){
			System.out.println("retry======>"+e.getMessage());
            while (count < maxRetryCount){
                count++;
                return retry(chain);
            }
        }
		MediaType mediaType = response.body().contentType();
		return response.newBuilder().body(ResponseBody.create(mediaType, respBytes)).build();
    }


	private String getRequestBody(Request request) {
		         String requestContent = "";
		         if (request == null) {
			             return requestContent;
			         }
		         RequestBody requestBody = request.body();
		         if (requestBody == null) {
			             return requestContent;
			         }
		         try {
			             Buffer buffer = new Buffer();
			             requestBody.writeTo(buffer);
			             Charset charset = Charset.forName("utf-8");
			             requestContent = buffer.readString(charset);
			        } catch (IOException e) {
			             e.printStackTrace();
			         }
		         return requestContent;
		     }
}
