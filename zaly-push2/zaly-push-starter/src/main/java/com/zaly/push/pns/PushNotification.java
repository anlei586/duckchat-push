package com.zaly.push.pns;

import com.zaly.push.constant.AppEnum;
import com.zaly.push.pns.apns.APNsNotification;
import com.zaly.push.pns.apns.APNsPackage;
import com.zaly.push.pns.huawei.HmsPackage;
import com.zaly.push.pns.huawei.PushHMSNotification;
import com.zaly.push.pns.umeng.PushUmengNotification;
import com.zaly.push.pns.umeng.UmengPackage;
import com.zaly.push.pns.xiaomi.PushXiaomiNotification;
import com.zaly.push.pns.xiaomi.XiaomiPackage;

public class PushNotification {
	/**
	 * 发送APNs消息通知PUSH
	 * 
	 * @param pack
	 */
	public static void pushAPNsNotification(AppEnum app,APNsPackage apk) {
		APNsNotification.getInstance().pushNotification(apk);
	}

	/**
	 * send xiaomi notification
	 * 
	 * @param app
	 * @param pack
	 */
	public static void pushXiaomiNotification(AppEnum app, XiaomiPackage xpk) {
		PushXiaomiNotification.getInstance().send(app, xpk);
	}

	/**
	 * send hms notification
	 * 
	 * @param app
	 * @param hpk
	 */
	public static void pushHmsNotification(AppEnum app, HmsPackage hpk) {
		PushHMSNotification.getInstance().send(app, hpk);
	}

	/**
	 * send umeng notification
	 * 
	 * @param app
	 * @param upk
	 */
	public static void pushUMengNotification(AppEnum app, UmengPackage upk) {
		PushUmengNotification.getInstance().send(app, upk);
	}

}
