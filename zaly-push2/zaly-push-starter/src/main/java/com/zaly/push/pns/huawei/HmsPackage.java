package com.zaly.push.pns.huawei;

import java.util.List;
import java.util.Map;

import com.akaxin.platform.common.utils.GsonUtils;
import com.zaly.proto.platform.Common;
import com.zaly.proto.platform.Common.PayloadType;
import com.zaly.push.pns.IPushPackage;

public class HmsPackage implements IPushPackage {

	private String packageName;

	private String pushToken;
	private List<String> pushTokenList;

	private String title;
	private String content;

	private String soundUri;//
	private int badge = 1;// 气泡数量
	private String pushGoto;
	private String pic;

	// 打开网页，
	// 打开富媒体，
	// 打开应用 => 3,类型3为打开APP，其他行为请参考接口文档设置
	private int actionType = 3;

	private Map<String, Object> extraFields;

	/**
	 * @return the packageName
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * @param packageName
	 *            the packageName to set
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * @return the pushToken
	 */
	public String getPushToken() {
		return pushToken;
	}

	/**
	 * @param pushToken
	 *            the pushToken to set
	 */
	public void setPushToken(String pushToken) {
		this.pushToken = pushToken;
	}

	/**
	 * @return the pushTokenList
	 */
	public List<String> getPushTokenList() {
		return pushTokenList;
	}

	/**
	 * @param pushTokenList
	 *            the pushTokenList to set
	 */
	public void setPushTokenList(List<String> pushTokenList) {
		this.pushTokenList = pushTokenList;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content
	 *            the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return the soundUri
	 */
	public String getSoundUri() {
		return soundUri;
	}

	/**
	 * @param soundUri
	 *            the soundUri to set
	 */
	public void setSoundUri(String soundUri) {
		this.soundUri = soundUri;
	}

	/**
	 * @return the badge
	 */
	public int getBadge() {
		return badge;
	}

	/**
	 * @param badge
	 *            the badge to set
	 */
	public void setBadge(int badge) {
		this.badge = badge;
	}

	/**
	 * @return the pushGoto
	 */
	public String getPushGoto() {
		return pushGoto;
	}

	/**
	 * @param pushGoto
	 *            the pushGoto to set
	 */
	public void setPushGoto(String pushGoto) {
		this.pushGoto = pushGoto;
	}

	/**
	 * @return the pic
	 */
	public String getPic() {
		return pic;
	}

	/**
	 * @param pic
	 *            the pic to set
	 */
	public void setPic(String pic) {
		this.pic = pic;
	}

	/**
	 * @return the extraFields
	 */
	public Map<String, Object> getExtraFields() {
		return extraFields;
	}

	/**
	 * @param extraFields
	 *            the extraFields to set
	 */
	public void setExtraFields(Map<String, Object> extraFields) {
		this.extraFields = extraFields;
	}

	/**
	 * @return the actionType
	 */
	public int getActionType() {
		return actionType;
	}

	@Override
	public String toString() {
		return GsonUtils.toJson(this);
	}

	@Override
	public PayloadType getType() {
		return Common.PayloadType.HUAWEI;
	}

}
