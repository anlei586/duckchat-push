package com.zaly.push.pns.xiaomi;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.akaxin.platform.common.utils.GsonUtils;
import com.xiaomi.xmpush.server.Message;
import com.zaly.proto.platform.Common;
import com.zaly.proto.platform.Common.PayloadType;
import com.zaly.push.constant.PushConst;
import com.zaly.push.pns.IPushPackage;

public class XiaomiPackage implements IPushPackage {

	private String pushToken;
	private String title; // 通知栏展示的通知的标题。
	private String description;// 通知栏展示的通知的描述。
	private String payload; // 消息的内容。（注意：需要对payload字符串做urlencode处理）
	private int passThrough = 0;// DEFAULT_ALL = -1; DEFAULT_SOUND = 1; 使用默认提示音提示； DEFAULT_VIBRATE = 2;
								// 使用默认震动提示； DEFAULT_LIGHTS = 4; 使用默认led灯光提示；
	private int notifyType = 1;// 使用默认提示音提示；
	private long timeToLive = 1 * 60 * 60 * 1000l;// 可选项。如果用户离线，设置消息在服务器保存的时间，单位：ms。服务器默认最长保留两周。
	private int notifyId = 1000;// 默认情况下，通知栏只显示一条推送消息。如果通知栏要显示多条推送消息，需要针对不同的消息设置不同的notify_id（相同notify_id的通知栏消息会覆盖之前的）。
	private String extraSoundUri;// 自定义通知栏消息铃声。extra.sound_uri的值设置为铃声的URI。参考2.2.1注：铃声文件放在Android app的raw目录下
	private int badge = 1;// 气泡数量
	private String pushGoto;
	private String restrictedPackageName;

	private Map<String, Object> extraFields = new HashMap<String, Object>();

	public String getPushToken() {
		return pushToken;
	}

	public void setPushToken(String pushToken) {
		this.pushToken = pushToken;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getPassThrough() {
		return passThrough;
	}

	public void setPassThrough(int passThrough) {
		this.passThrough = passThrough;
	}

	public long getTimeToLive() {
		return timeToLive;
	}

	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	public int getBadge() {
		return badge;
	}

	public void setBadge(int badge) {
		this.badge = badge;
	}

	public Map<String, Object> getExtraFields() {
		return extraFields;
	}

	public void setExtraFields(Map<String, Object> extraFields) {
		this.extraFields = extraFields;
	}

	public String getRestrictedPackageName() {
		return restrictedPackageName;
	}

	public void setRestrictedPackageName(String restrictedPackageName) {
		this.restrictedPackageName = restrictedPackageName;
	}

	public int getNotifyType() {
		return notifyType;
	}

	public void setNotifyType(int notifyType) {
		this.notifyType = notifyType;
	}

	public int getNotifyId() {
		Random random = new Random();
		this.notifyId = random.nextInt(10000);
		return notifyId;
	}

	public void setNotifyId(int notifyId) {
		this.notifyId = notifyId;
	}

	public String getExtraSoundUri() {
		return extraSoundUri;
	}

	public void setExtraSoundUri(String extraSoundUri) {
		this.extraSoundUri = extraSoundUri;
	}

	public String getPushGoto() {
		return pushGoto;
	}

	public void setPushGoto(String pushGoto) {
		this.pushGoto = pushGoto;
	}

	public Message buildMessage() {
		Message.Builder messageBuilder = new Message.Builder()//
				.title(this.title)//
				.description(this.description)//
				.payload(this.payload)//
				.notifyType(this.notifyType)//
				.restrictedPackageName(this.restrictedPackageName)//
				.timeToLive(this.timeToLive)//
				.passThrough(this.passThrough)//
				.notifyId(this.getNotifyId());

		if (StringUtils.isNotEmpty(this.pushGoto)) {
			messageBuilder.extra(PushConst.GOTO_URL, this.pushGoto);
		}
		messageBuilder.extra("EXTRA_PARAM_NOTIFY_FOREGROUND", "0");
		return messageBuilder.build();
	}

	@Override
	public String toString() {
		return GsonUtils.toJson(this);
	}

	@Override
	public PayloadType getType() {
		return Common.PayloadType.XIAOMI;
	}

}
