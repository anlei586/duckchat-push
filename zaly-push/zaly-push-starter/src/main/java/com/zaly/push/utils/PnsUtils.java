package com.zaly.push.utils;

import org.apache.commons.lang3.StringUtils;

import com.zaly.proto.platform.Common;
import com.zaly.push.pns.apns.APNsPackage;
import com.zaly.push.pns.huawei.HmsPackage;
import com.zaly.push.pns.umeng.UmengPackage;
import com.zaly.push.pns.xiaomi.XiaomiPackage;

/**
 * build package for pns
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-08-08 11:43:17
 */
public class PnsUtils {

	public static APNsPackage buidApns(Common.Payload payload) {
		APNsPackage apk = new APNsPackage();
		apk.setToken(payload.getToken());

		apk.setTitle(payload.getTitle());
		apk.setBody(payload.getBody());
		apk.setPushGoto(payload.getPushGoto());

		apk.setBadge(payload.getBadge());
		// apk.setSound(payload.getSound());

		return apk;
	}

	public static XiaomiPackage buidXiaomi(Common.Payload payload) {
		XiaomiPackage xpk = new XiaomiPackage();
		xpk.setPushToken(payload.getToken());

		xpk.setTitle(payload.getTitle());
		xpk.setDescription(payload.getBody());
		xpk.setPushGoto(payload.getPushGoto());

		xpk.setBadge(payload.getBadge());
		// xpk.setExtraSoundUri(payload.getSound());
		return xpk;
	}

	public static HmsPackage buidHuawei(Common.Payload payload) {
		HmsPackage hpk = new HmsPackage();
		hpk.setPushToken(payload.getToken());
		hpk.setTitle(payload.getTitle());
		if (StringUtils.isNotEmpty(payload.getSubTitle())) {
			hpk.setContent("[" + payload.getSubTitle() + "]" + payload.getBody());
		} else {
			hpk.setContent(payload.getBody());
		}
		hpk.setPushGoto(payload.getPushGoto());
		hpk.setBadge(payload.getBadge());
		return hpk;
	}

	public static UmengPackage buidUMeng(Common.Payload payload) {
		UmengPackage upk = new UmengPackage();
		upk.setPushToken(payload.getToken());

		upk.setTitle(payload.getTitle());
		upk.setText(payload.getBody());
		upk.setTicker(payload.getBody());
		upk.setPushGoto(payload.getPushGoto());

		return upk;
	}

}
