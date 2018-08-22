package com.zaly.push.pns.apns;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.akaxin.platform.common.utils.GsonUtils;
import com.zaly.proto.platform.Common;
import com.zaly.proto.platform.Common.PayloadType;
import com.zaly.push.apns.notification.Payload;
import com.zaly.push.apns.notification.PayloadBuilder;
import com.zaly.push.constant.PushConst;
import com.zaly.push.pns.IPushPackage;

/**
 * APNs的消息结构包
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-01-23 17:24:14
 */
public class APNsPackage implements IPushPackage {

	private String token;
	private String title;
	private String subtitle;
	private String body;
	private int badge;
	private String category;
	private String sound = "default.caf";
	private String pushGoto;

	private Map<String, Object> alertExtraFields = new HashMap<String, Object>();
	private Map<String, Object> apsExtraFields = new HashMap<String, Object>();
	private Map<String, Object> rootExtraFields = new HashMap<String, Object>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the subtitle
	 */
	public String getSubtitle() {
		return subtitle;
	}

	/**
	 * @param subtitle
	 *            the subtitle to set
	 */
	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public int getBadge() {
		return badge;
	}

	public APNsPackage setBadge(int badge) {
		this.badge = badge;
		return this;
	}

	public String getSound() {
		return sound;
	}

	public APNsPackage setSound(String sound) {
		this.sound = sound;
		return this;
	}

	public String getPushGoto() {
		return pushGoto;
	}

	public void setPushGoto(String pushGoto) {
		this.pushGoto = pushGoto;
	}

	public String buildPayloadJson() {

		PayloadBuilder payLoadBuilder = Payload.newPayload();

		if (title != null) {
			payLoadBuilder.addTitle(title);
		}
		if (subtitle != null) {
			payLoadBuilder.addSubTitle(subtitle);
		}
		if (body != null) {
			payLoadBuilder.addAlertBody(body);
		}
		if (badge > 0) {
			payLoadBuilder.addBadge(badge);
		}
		if (sound != null) {
			payLoadBuilder.addSound(sound);
		}

		if (StringUtils.isNotEmpty(this.pushGoto)) {
			apsExtraFields.put(PushConst.GOTO_URL, this.pushGoto);
		}

		if (alertExtraFields != null && alertExtraFields.size() > 0) {
			payLoadBuilder.addCustomAlertFields(alertExtraFields);
		}
		if (apsExtraFields != null && apsExtraFields.size() > 0) {
			payLoadBuilder.addCustomApsField(apsExtraFields);
		}
		if (rootExtraFields != null && rootExtraFields.size() > 0) {
			payLoadBuilder.addRootExtraFields(rootExtraFields);
		}

		if (StringUtils.isNotEmpty(category)) {
			payLoadBuilder.addCustomApsField("category", category);
		}

		return payLoadBuilder.build();
	}

	public Map<String, Object> getRootExtraFields() {
		return rootExtraFields;
	}

	public void setRootExtraFields(Map<String, Object> rootExtraFields) {
		this.rootExtraFields = rootExtraFields;
	}

	public void setRootExtraField(String key, Object value) {
		this.rootExtraFields.put(key, value);
	}

	public Map<String, Object> getAlertExtraFields() {
		return alertExtraFields;
	}

	public void setAlertExtraFields(Map<String, Object> alertExtraFields) {
		this.alertExtraFields = alertExtraFields;
	}

	public void setAlertExtraField(String key, Object value) {
		this.alertExtraFields.put(key, value);
	}

	public Map<String, Object> getApsExtraFields() {
		return apsExtraFields;
	}

	public void setApsExtraFields(Map<String, Object> apsExtraFields) {
		this.apsExtraFields = apsExtraFields;
	}

	public void setApsExtraField(String key, Object value) {
		this.apsExtraFields.put(key, value);
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public String toString() {
		return GsonUtils.toJson(this);
	}

	@Override
	public PayloadType getType() {
		return Common.PayloadType.IOS;
	}

}
