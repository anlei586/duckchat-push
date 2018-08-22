package com.zaly.push.pns.xiaomi;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akaxin.platform.common.utils.StringHelper;
import com.xiaomi.xmpush.server.Message;
import com.xiaomi.xmpush.server.Result;
import com.zaly.push.constant.AppEnum;
import com.zaly.push.pns.IPushNotification;
import com.zaly.push.pns.IPushPackage;

/**
 * 推送小米push
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-05-04 19:19:40
 */
public class PushXiaomiNotification implements IPushNotification {
	private static final Logger logger = LoggerFactory.getLogger(PushXiaomiNotification.class);

	// akaxin
	private static final String AKAXIN_PACKAGE_NAME = "com.akaxin.client";
	private static final String AKAXIN_SECRET_KEY = "S5BPUWhx4v7F3bxMHoaOfA==";

	private static final String AKAXIN_PACKAGE_NAME_DEBUG = "com.akaxin.client.debug";
	private static final String AKAXIN_SECRET_KEY_DEBUG = "m3s/b/KvbKjSZaep1zE2Zw==";

	// duckchat
	private static final String DUCKCHAT_PACKAGE_NAME = "com.akaxin.zaly";
	private static final String DUCKCHAT_SECRET_KEY = "4gwLruEYbHHmCPTz2qG+bQ==";

	private static final String DUCKCHAT_PACKAGE_NAME_DEBUG = "com.akaxin.zaly.debug";
	private static final String DUCKCHAT_SECRET_KEY_DEBUG = "uNaJP3iseFbFRAVG96AcCg==";

	private static final String SANBOX_PRE = "dev_";

	private PushXiaomiNotification() {

	}

	private static class SingletonHolder {
		private static PushXiaomiNotification instance = new PushXiaomiNotification();
	}

	public static PushXiaomiNotification getInstance() {
		return SingletonHolder.instance;
	}

	@Override
	public void send(AppEnum app, IPushPackage iPack) {
		XiaomiPackage xiaomiPack = (XiaomiPackage) iPack;

		try {

			logger.info("xiaomi push app={} pack={}", app, xiaomiPack.toString());

			String appSecretKey = null;
			String xiaomiToken = xiaomiPack.getPushToken();
			boolean isSandbox = false;

			if (StringUtils.isNotEmpty(xiaomiToken) && xiaomiToken.startsWith(SANBOX_PRE)) {
				isSandbox = true;// debug测试环境
				xiaomiToken = xiaomiToken.substring(4, xiaomiToken.length());

				switch (app) {
				case AKAXIN:
					appSecretKey = AKAXIN_SECRET_KEY_DEBUG;
					xiaomiPack.setRestrictedPackageName(AKAXIN_PACKAGE_NAME_DEBUG);
					break;
				case DUCKCHAT:
					appSecretKey = DUCKCHAT_SECRET_KEY_DEBUG;
					xiaomiPack.setRestrictedPackageName(DUCKCHAT_PACKAGE_NAME_DEBUG);
					logger.info("xiaomi push duckchat => sandbox ");
					break;
				default:
					logger.error("xiaomi push error app type => sandbox ");
					throw new Exception("xiaomi push error app type => sandbox ");
					// return;
				}

			} else {

				switch (app) {
				case AKAXIN:
					appSecretKey = AKAXIN_SECRET_KEY;
					xiaomiPack.setRestrictedPackageName(AKAXIN_PACKAGE_NAME);
					logger.info("xiaomi push akaxin => official ");
					break;
				case DUCKCHAT:
					appSecretKey = DUCKCHAT_SECRET_KEY;
					xiaomiPack.setRestrictedPackageName(DUCKCHAT_PACKAGE_NAME);
					logger.info("xiaomi push duckchat => official ");
					break;
				default:
					logger.error("xiaomi push error app type => official ");
					throw new Exception("xiaomi push error app type => official ");
					// return;
				}

			}

			logger.info("start to send xiaomi push");

			Message message = xiaomiPack.buildMessage();

			logger.info("start to send xiaomi push message={}", message.toString());

			Result result = XiaomiPushClient.pushMessage(appSecretKey, xiaomiToken, message);

			logger.info("send xiaomi push isSandbox={} result={}", isSandbox, result);
		} catch (Exception e) {
			logger.error("send xiaomi push error", e);
		}
	}

}
