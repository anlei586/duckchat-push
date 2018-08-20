package com.zaly.push.apns.notification;

import java.util.Date;

/**
 * @author frank@linkedkeeper.com on 2016/12/29.
 */
public interface IApnsPushNotificationResponse<T extends IApnsPushNotification> {

    T getApnsPushNotification();

    boolean isSuccess();

    String getRejectionReason();

    Date getTokenInvalidationTimestamp();

}
