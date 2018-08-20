package com.zaly.push.apns.notification;

import java.util.Date;

import com.google.gson.Gson;

/**
 * @author frank@linkedkeeper.com on 2016/12/29.
 */
public class ApnsHttp2PushNotificationResponse<T extends IApnsPushNotification>
		implements IApnsPushNotificationResponse<T> {

	private final T apnsPushNotification;
	private final boolean success;
	private final String rejectionReason;
	private final Date tokenExpirationTimestamp;

	public ApnsHttp2PushNotificationResponse(T apnsPushNotification, boolean success, String rejectionReason,
			Date tokenExpirationTimestamp) {
		this.apnsPushNotification = apnsPushNotification;
		this.success = success;
		this.rejectionReason = rejectionReason;
		this.tokenExpirationTimestamp = tokenExpirationTimestamp;
	}

	@Override
	public T getApnsPushNotification() {
		return this.apnsPushNotification;
	}

	@Override
	public boolean isSuccess() {
		return this.success;
	}

	@Override
	public String getRejectionReason() {
		return this.rejectionReason;
	}

	@Override
	public Date getTokenInvalidationTimestamp() {
		return this.tokenExpirationTimestamp;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
