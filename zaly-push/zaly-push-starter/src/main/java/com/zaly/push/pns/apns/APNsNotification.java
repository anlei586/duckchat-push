package com.zaly.push.pns.apns;

import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaly.push.apns.client.IApnsHttp2Client;
import com.zaly.push.apns.notification.IApnsPushNotification;
import com.zaly.push.apns.notification.IApnsPushNotificationResponse;

/**
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-01-23 18:17:29
 */
public class APNsNotification {
	private static final Logger logger = LoggerFactory.getLogger(APNsNotification.class);
	private static final String SANBOX_PRE = "dev_";

	private static class SingletonHolder {
		private static APNsNotification instance = new APNsNotification();
	}

	public static APNsNotification getInstance() {
		return SingletonHolder.instance;
	}

	private APNsNotification() {

	}

	public boolean disconnectAPNs() {
		try {
			IApnsHttp2Client apnsHttp2Client = APNsPushManager.getInstance().getApnsClient(false);
			apnsHttp2Client.disconnect();
			return true;
		} catch (Exception e) {
			logger.error("disconnct apns connection error", e);
		}
		return false;
	}

	public boolean pushNotification(APNsPackage apnsPack) {
//		logger.info("token={} payload={}", apnsPack.getToken(), apnsPack.buildPayloadJson());
		return sendPayload(apnsPack.getToken(), apnsPack.buildPayloadJson());
	}

	private boolean sendPayload(String apnsToken, String payload) {
		boolean isSandboxEnv = false;
		try {
			// 需要通过token，判断是否为开发版本/sandbox/develop/测试版本
			if (StringUtils.isNotBlank(apnsToken) && apnsToken.startsWith(SANBOX_PRE)) {
				logger.info("send APNs push by DEV model token={}", apnsToken);
				isSandboxEnv = true;
				apnsToken = apnsToken.substring(4, apnsToken.length());
			}

			IApnsHttp2Client apnsHttp2Client = APNsPushManager.getInstance().getApnsClient(isSandboxEnv);
			Future<IApnsPushNotificationResponse<IApnsPushNotification>> response = apnsHttp2Client
					.pushMessageAsync(apnsToken, payload);
			logger.info("send payload isSandboxEnv={} response={}", isSandboxEnv, response.get());
			return true;
		} catch (Exception e) {
			logger.error("send payload error", e);
		}

		return false;
	}

}
