package com.zaly.push.pns.umeng;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaly.push.constant.AppEnum;
import com.zaly.push.constant.PushConst;
import com.zaly.push.pns.IPushNotification;
import com.zaly.push.pns.IPushPackage;
import com.zaly.push.pns.PushResult;

/**
 * 推送umeng-Push
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-05-04 18:44:48
 */
public class PushUmengNotification implements IPushNotification {
	private static final Logger logger = LoggerFactory.getLogger(PushUmengNotification.class);

	private static final String AKAXIN_UMENG_APP_KEY = "5aeaae6bf29d9812810000b1";
	private static final String AKAXIN_UMENG_APP_MASTER_SECRET = "b9xs50emaqzf5fzfbclm9qbupq2gyeul";

	private static final String AKAXIN_UMENG_APP_KEY_DEBUG = "5aeaadf98f4a9d06c200010b";
	private static final String AKAXIN_UMENG_APP_MASTER_SECRET_DEBUG = "ahwsqtjdtr9dku1mzx0sm3bq7arro7rz";

	private static final String DUCKCHAT_UMENG_APP_KEY = "5b72547bf43e4827ee00001d";
	private static final String DUCKCHAT_UMENG_APP_MASTER_SECRET = "ejyj62ezgcmufwmnfewobrhkxhbu3wgb";

	private static final String DUCKCHAT_UMENG_APP_KEY_DEBUG = "5b72551ff43e482e33000015";
	private static final String DUCKCHAT_UMENG_APP_MASTER_SECRET_DEBUG = "qv6edl8pj5qnpuzyr6ckzlvm37gom3ve";

	private static final String SANBOX_PRE = "dev";

	private UmengPushClient umengPushclient = new UmengPushClient();

	private PushUmengNotification() {

	}

	private static class SingletonHolder {
		private static PushUmengNotification instance = new PushUmengNotification();
	}

	public static PushUmengNotification getInstance() {
		return SingletonHolder.instance;
	}

	// 通过客户端，执行push操作
	@Override
	public void send(AppEnum app, IPushPackage ipack) {
		try {
			UmengPackage umPack = (UmengPackage) ipack;
			String pushToken = umPack.getPushToken();

			if (StringUtils.isEmpty(pushToken)) {
				return;
			}

			AndroidUnicast unicast = null;
			if (StringUtils.isNotEmpty(pushToken) && pushToken.startsWith(SANBOX_PRE)) {
				pushToken = pushToken.substring(4, pushToken.length());

				switch (app) {
				case AKAXIN:
					unicast = new AndroidUnicast(AKAXIN_UMENG_APP_KEY_DEBUG, AKAXIN_UMENG_APP_MASTER_SECRET_DEBUG);
					break;
				case DUCKCHAT:
					unicast = new AndroidUnicast(DUCKCHAT_UMENG_APP_KEY_DEBUG, DUCKCHAT_UMENG_APP_MASTER_SECRET_DEBUG);
					break;
				default:
					return;
				}
				unicast.setTestMode();

			} else {

				switch (app) {
				case AKAXIN:
					unicast = new AndroidUnicast(AKAXIN_UMENG_APP_KEY, AKAXIN_UMENG_APP_MASTER_SECRET);
					break;
				case DUCKCHAT:
					unicast = new AndroidUnicast(DUCKCHAT_UMENG_APP_KEY, DUCKCHAT_UMENG_APP_MASTER_SECRET);
					break;
				default:
					return;
				}
				unicast.setProductionMode();
			}
			unicast.setDeviceToken(pushToken);
			unicast.setTicker(umPack.getTicker());
			unicast.setTitle(umPack.getTitle());
			unicast.setText(umPack.getText());
			unicast.setPlaySound(true);
			unicast.goAppAfterOpen();
			unicast.setDisplayType(AndroidNotification.DisplayType.NOTIFICATION);

			unicast.setExtraField(PushConst.GOTO_URL, umPack.getPushGoto());

			PushResult result = umengPushclient.send(unicast);

			logger.info("send U-MENG push body={} result={}", unicast.getPostBody(), result.toString());
		} catch (Exception e) {
			logger.error("push umeng notification error", e);
		}

	}
}
