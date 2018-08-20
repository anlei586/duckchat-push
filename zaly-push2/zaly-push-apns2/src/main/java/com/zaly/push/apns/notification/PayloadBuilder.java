package com.zaly.push.apns.notification;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a builder for constructing Payload requests, as specified by Apple
 * Push Notification Programming Guide.
 * 
 * 
 *
 * @author frank@linkedkeeper.com on 2016/12/29.
 */
public final class PayloadBuilder {

	private static final ObjectMapper mapper = new ObjectMapper();

	private final Map<String, Object> root;
	private final Map<String, Object> aps;
	private final Map<String, Object> customAlert;// 放在alter中
	private final Map<String, Object> rootExtra;

	/**
	 * Constructs a new instance of {@code PayloadBuilder}
	 */
	public PayloadBuilder() {
		this.root = new HashMap<>();
		this.aps = new HashMap<>();
		this.customAlert = new HashMap<>();
		this.rootExtra = new HashMap<>();
	}

	public PayloadBuilder addAlertTitle(final String title) {
		customAlert.put("title", title);
		return this;
	}

	/**
	 * Sets the alert body text, the text the appears to the user, to the passed
	 * value
	 *
	 * @param alert
	 *            the text to appear to the user
	 * @return this
	 */
	public PayloadBuilder addAlertBody(final String body) {
		customAlert.put("body", body);
		return this;
	}

	public PayloadBuilder addLaunchImage(final String launchImage) {
		customAlert.put("launch-image", launchImage);
		return this;
	}

	/**
	 * add Extra Field to Alert Map
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public PayloadBuilder addCustomAlertField(String key, Object value) {
		customAlert.put(key, value);
		return this;
	}

	public PayloadBuilder addCustomAlertFields(Map<String, Object> map) {
		customAlert.putAll(map);
		return this;
	}

	/**
	 * Sets the notification badge to be displayed next to the application icon.
	 * <p>
	 * The passed value is the value that should be displayed (it will be added to
	 * the previous badge number), and a badge of 0 clears the badge indicator.
	 *
	 * @param badge
	 *            the badge number to be displayed
	 * @return this
	 */
	public PayloadBuilder addBadge(final int badge) {
		aps.put("badge", badge);
		return this;
	}

	public PayloadBuilder addSound(String sound) {
		aps.put("sound", sound);
		return this;
	}

	public PayloadBuilder addCustomApsField(String key, Object value) {
		aps.put(key, value);
		return this;
	}

	public PayloadBuilder addCustomApsField(Map<String, Object> map) {
		aps.putAll(map);
		return this;
	}

	/**
	 * 增加图片以及其他文件支持
	 * 
	 * @param filePath
	 *            图片以及其他文件地址
	 * @param type
	 *            图片以及其他文件类型
	 * @return
	 */
	public PayloadBuilder addExtraFileApsField(String filePath, int type) {
		aps.put("mutable-content", 1);
		aps.put("file-path", filePath);
		aps.put("file-type", type);
		return this;
	}

	public PayloadBuilder addRootExtraField(String key, Object value) {
		rootExtra.put(key, value);
		return this;
	}

	public PayloadBuilder addRootExtraFields(Map<String, Object> map) {
		rootExtra.putAll(map);
		return this;
	}

	/**
	 * Returns the JSON String representation of the payload according to Apple APNS
	 * specification
	 *
	 * @return the String representation as expected by Apple
	 */
	public String build() {
		if (!root.containsKey("mdm")) {
			insertCustomAlert();
			root.put("aps", aps);
			if (rootExtra != null) {
				for (String key : rootExtra.keySet()) {
					root.put(key, rootExtra.get(key));
				}
			}
		}
		try {
			return mapper.writeValueAsString(root);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void insertCustomAlert() {
		switch (customAlert.size()) {
		case 0:
			aps.remove("alert");
			break;
		case 1:// 兼容只发送文案
			if (customAlert.containsKey("body")) {
				aps.put("alert", customAlert.get("body"));
				break;
			}
		default:// 可以发送title+body
			aps.put("alert", customAlert);
		}
	}
}
