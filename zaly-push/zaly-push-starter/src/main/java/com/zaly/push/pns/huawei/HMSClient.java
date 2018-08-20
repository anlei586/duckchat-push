package com.zaly.push.pns.huawei;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class HMSClient {

	// 用户在华为开发者联盟申请的appId和appSecret（会员中心->我的产品，点击产品对应的Push服务，点击“移动应用详情”获取）
	private static String tokenUrl = "https://login.cloud.huawei.com/oauth2/v2/token"; // 获取认证Token的URL
	private static String apiUrl = "https://api.push.hicloud.com/pushsend.do"; // 应用级消息下发API
	private static String accessToken;// 下发通知消息的认证Token
	private static long tokenExpiredTime; // accessToken的过期时间

	private static final String PUSH_GOTO = "push-goto";

	public static void main(String[] args) throws IOException {
		// sendPushMessage();
	}

	// 获取下发通知消息的认证Token
	private static void refreshToken(String appId, String appSecret) throws IOException {
		String msgBody = MessageFormat.format("grant_type=client_credentials&client_secret={0}&client_id={1}",
				URLEncoder.encode(appSecret, "UTF-8"), appId);
		String response = httpPost(tokenUrl, msgBody, 5000, 5000);
		JSONObject obj = JSONObject.parseObject(response);
		accessToken = obj.getString("access_token");
		tokenExpiredTime = System.currentTimeMillis() + obj.getLong("expires_in") - 5 * 60 * 1000;
	}

	// 发送Push消息
	public static void sendPushMessage(String appId, String appSecret, HmsPackage hpack) throws Exception {

		if (hpack == null) {
			throw new Exception("push to HMS with null package");
		}

		if (tokenExpiredTime <= System.currentTimeMillis()) {
			refreshToken(appId, appSecret);
		}
		/* PushManager.requestToken为客户端申请token的方法，可以调用多次以防止申请token失败 */
		/* PushToken不支持手动编写，需使用客户端的onToken方法获取 */
		JSONArray deviceTokens = new JSONArray();// 目标设备Token

		if (StringUtils.isNotEmpty(hpack.getPushToken())) {
			deviceTokens.add(hpack.getPushToken());
		} else {
			for (String token : hpack.getPushTokenList()) {
				deviceTokens.add(token);
			}
		}

		JSONObject body = new JSONObject();// 仅通知栏消息需要设置标题和内容，透传消息key和value为用户自定义
		body.put("title", hpack.getTitle());// 消息标题
		body.put("content", hpack.getContent());// 消息内容体

		JSONObject param = new JSONObject();
		param.put("appPkgName", hpack.getPackageName());// 定义需要打开的appPkgName

		JSONObject action = new JSONObject();

		// 打开网页，
		// 打开富媒体，
		// 打开应用 => 3,类型3为打开APP，其他行为请参考接口文档设置
		action.put("type", hpack.getActionType());
		action.put("param", param);// 消息点击动作参数

		JSONObject msg = new JSONObject();

		msg.put("type", 3);// 3: 通知栏消息，异步透传消息请根据接口文档设置
		msg.put("action", action);// 消息点击动作
		msg.put("body", body);// 通知栏消息body内容

		JSONObject ext = new JSONObject();// 扩展信息，含BI消息统计，特定展示风格，消息折叠。
		// ext.put("biTag", "Trump");// 设置消息标签，如果带了这个标签，会在回执中推送给CP用于检测某种类型消息的到达率和状态
		ext.put("icon", hpack.getPic());// 自定义推送消息在通知栏的图标,value为一个公网可以访问的URL
		ext.put(PUSH_GOTO, hpack.getPushGoto());

		if (hpack.getExtraFields() != null && hpack.getExtraFields().size() > 0) {
			ext.putAll(hpack.getExtraFields());
		}

		JSONObject hps = new JSONObject();// 华为PUSH消息总结构体
		hps.put("msg", msg);
		hps.put("ext", ext);

		JSONObject payload = new JSONObject();
		payload.put("hps", hps);

		String postBody = MessageFormat.format(
				"access_token={0}&nsp_svc={1}&nsp_ts={2}&device_token_list={3}&payload={4}",
				URLEncoder.encode(accessToken, "UTF-8"), URLEncoder.encode("openpush.message.api.send", "UTF-8"),
				URLEncoder.encode(String.valueOf(System.currentTimeMillis() / 1000), "UTF-8"),
				URLEncoder.encode(deviceTokens.toString(), "UTF-8"), URLEncoder.encode(payload.toString(), "UTF-8"));

		String postUrl = apiUrl + "?nsp_ctx="
				+ URLEncoder.encode("{\"ver\":\"1\", \"appId\":\"" + appId + "\"}", "UTF-8");
		httpPost(postUrl, postBody, 5000, 5000);// 5s 超时
	}

	private static String httpPost(String httpUrl, String data, int connectTimeout, int readTimeout) throws IOException {
		OutputStream outPut = null;
		HttpURLConnection urlConnection = null;
		InputStream in = null;

		try {
			URL url = new URL(httpUrl);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			urlConnection.setConnectTimeout(connectTimeout);
			urlConnection.setReadTimeout(readTimeout);
			urlConnection.connect();

			// POST data
			outPut = urlConnection.getOutputStream();
			outPut.write(data.getBytes("UTF-8"));
			outPut.flush();

			// read response
			if (urlConnection.getResponseCode() < 400) {
				in = urlConnection.getInputStream();
			} else {
				in = urlConnection.getErrorStream();
			}

			List<String> lines = IOUtils.readLines(in, urlConnection.getContentEncoding());
			StringBuffer strBuf = new StringBuffer();
			for (String line : lines) {
				strBuf.append(line);
			}
			System.out.println(strBuf.toString());
			return strBuf.toString();
		} finally {
			IOUtils.closeQuietly(outPut);
			IOUtils.closeQuietly(in);
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}
}
