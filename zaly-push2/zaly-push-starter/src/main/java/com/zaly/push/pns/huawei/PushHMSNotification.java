package com.zaly.push.pns.huawei;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaly.push.constant.AppEnum;
import com.zaly.push.pns.IPushNotification;
import com.zaly.push.pns.IPushPackage;

public class PushHMSNotification implements IPushNotification {
	private static final Logger logger = LoggerFactory.getLogger(PushHMSNotification.class);

	// duckchat - debug
	private static final String DUCKCHAT_PACKATE_NAME = "com.akaxin.zaly";

	private static final String DUCKCHAT_APP_ID = "100371601";

	private static final String DUCKCHAT_APP_SECRET = "97d3bbb9551395679f4a3b7b17190e2d";

	// duckchat - debug
	private static final String DUCKCHAT_PACKATE_NAME_DEBUG = "com.akaxin.zaly.debug";

	private static final String DUCKCHAT_APP_ID_DEBUG = "100378565";

	private static final String DUCKCHAT_APP_SECRET_DEBUG = "2324f173abb369ce90f11a706305e49b";

	private static final String DEV_PRE = "dev_";

	private static class SingletonHolder {
		private static PushHMSNotification instance = new PushHMSNotification();
	}

	public static PushHMSNotification getInstance() {
		return SingletonHolder.instance;
	}

	@Override
	public void send(AppEnum app, IPushPackage ipack) {
		try {
			HmsPackage hpack = (HmsPackage) ipack;
			String pushToken = hpack.getPushToken();
			String appId = null;
			String appSecret = null;
			if (StringUtils.isNotEmpty(pushToken) && pushToken.startsWith(DEV_PRE)) {
				pushToken = pushToken.substring(4, pushToken.length());

				switch (app) {
				case AKAXIN:

					break;
				case DUCKCHAT:
					hpack.setPackageName(DUCKCHAT_PACKATE_NAME_DEBUG);
					appId = DUCKCHAT_APP_ID_DEBUG;
					appSecret = DUCKCHAT_APP_SECRET_DEBUG;
					break;
				default:
					break;

				}

			} else {

				switch (app) {
				case AKAXIN:
					break;
				case DUCKCHAT:
					hpack.setPackageName(DUCKCHAT_PACKATE_NAME);
					appId = DUCKCHAT_APP_ID;
					appSecret = DUCKCHAT_APP_SECRET;
					break;
				default:
					break;

				}
			}
			HMSClient.sendPushMessage(appId, appSecret, hpack);
		} catch (Exception e) {
			logger.error("push hms notification error", e);
		}
	}

}
