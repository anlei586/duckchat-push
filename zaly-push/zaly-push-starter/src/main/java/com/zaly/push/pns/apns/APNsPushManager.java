package com.zaly.push.pns.apns;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaly.push.apns.client.ApnsHttp2Client;
import com.zaly.push.apns.client.IApnsHttp2Client;
import com.zaly.push.apns.constant.ApnsHttp2Config;
import com.zaly.push.constant.AppEnum;

/**
 * platform与APNs服务交互管理，发送
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-01-23 17:12:51
 */
public class APNsPushManager {
	private static final Logger logger = LoggerFactory.getLogger(APNsPushManager.class);

	private static final String AKAXIN_PUSH_NAME = "akaxin";
	private static final String AKZAXIN_OFFICIAL_CERT_FILE = "akaxin-apns-official-certificates.p12";
	private static final String AKZAXIN_OFFICIAL_PASSWD = "push#akaxin";
	private static final String AKZAXIN_SANDBOX_CERT_FILE = "akaxin-apns-sandbox-certificates.p12";
	private static final String AKZAXIN_SANDBOX_PASSWD = "123456";

	private static final String DUCKCHAT_PUSH_NAME = "duckchat";
	private static final String DUCKCHAT_OFFICIAL_CERT_FILE = "duckchat-apns-certificates.p12";
	private static final String DUCKCHAT_OFFICIAL_PASSWD = "push@duck.chat";
	private static final String DUCKCHAT_SANDBOX_CERT_FILE = "duckchat-apns-certificates.p12";
	private static final String DUCKCHAT_SANDBOX_PASSWD = "push@duck.chat";

	private static final int OFFICIAL_MAX_CONN = 4;
	private static final int SANDBOX_MAX_CONN = 2;

	private static APNsPushManager instance;
	private static IApnsHttp2Client officialApnsHttp2Client;
	private static IApnsHttp2Client sandboxApnsHttp2Client;

	private APNsPushManager() {

	}

	public static APNsPushManager getInstance() {
		if (instance == null) {
			instance = new APNsPushManager();
		}
		return instance;
	}

	public void start(AppEnum app) throws Exception {
		officialApnsHttp2Client = SingletonHolder.getOfficialApnsHttp2Client(app);
		sandboxApnsHttp2Client = SingletonHolder.getSandboxApnsHttp2Client(app);
		logger.info("start apns client client={} sandboxClient={}", officialApnsHttp2Client, sandboxApnsHttp2Client);
	}

	public IApnsHttp2Client getApnsClient(boolean isSandboxEnv) {
		return isSandboxEnv ? sandboxApnsHttp2Client : officialApnsHttp2Client;
	}

	private static class SingletonHolder {

		private static IApnsHttp2Client buildApnsHttp2Client(String pushName, String certFileName, String password,
				int maxApnsConnections, boolean isSandboxEnv) {
			IApnsHttp2Client client = null;
			if (client == null) {
				try {
					ApnsHttp2Config apnsHttp2Config = new ApnsHttp2Config();
					apnsHttp2Config.setName(pushName);
					if (StringUtils.isNotEmpty(pushName)) {
						apnsHttp2Config.setKeyStoreStream(pushName + "/" + certFileName);
					} else {
						apnsHttp2Config.setKeyStoreStream(certFileName);
					}
					apnsHttp2Config.setSandboxEnvironment(isSandboxEnv);
					apnsHttp2Config.setPassword(password);
					apnsHttp2Config.setPoolSize(maxApnsConnections);
					client = new ApnsHttp2Client(apnsHttp2Config);
				} catch (Exception e) {
					logger.error("build APNs Linked error", e);
				}
			}

			return client;
		}

		public static IApnsHttp2Client getOfficialApnsHttp2Client(AppEnum app) throws Exception {

			switch (app) {
			case AKAXIN:
				return buildApnsHttp2Client(AKAXIN_PUSH_NAME, AKZAXIN_OFFICIAL_CERT_FILE, AKZAXIN_OFFICIAL_PASSWD,
						OFFICIAL_MAX_CONN, false);
			case DUCKCHAT:
				return buildApnsHttp2Client(DUCKCHAT_PUSH_NAME, DUCKCHAT_OFFICIAL_CERT_FILE, DUCKCHAT_OFFICIAL_PASSWD,
						OFFICIAL_MAX_CONN, false);
			default:
				throw new Exception("start sandbox apns-client error");
			}
		}

		public static IApnsHttp2Client getSandboxApnsHttp2Client(AppEnum app) throws Exception {
			switch (app) {
			case AKAXIN:
				return buildApnsHttp2Client(AKAXIN_PUSH_NAME, AKZAXIN_SANDBOX_CERT_FILE, AKZAXIN_SANDBOX_PASSWD,
						SANDBOX_MAX_CONN, true);
			case DUCKCHAT:
				return buildApnsHttp2Client(DUCKCHAT_PUSH_NAME, DUCKCHAT_SANDBOX_CERT_FILE, DUCKCHAT_SANDBOX_PASSWD,
						SANDBOX_MAX_CONN, true);
			default:
				throw new Exception("start sandbox apns-client error");
			}
		}

	}

}
