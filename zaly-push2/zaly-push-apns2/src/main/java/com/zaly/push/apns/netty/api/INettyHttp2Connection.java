package com.zaly.push.apns.netty.api;

import java.util.Map;

import com.zaly.push.apns.netty.ErrorResponse;
import com.zaly.push.apns.notification.IApnsPushNotification;
import com.zaly.push.apns.notification.IApnsPushNotificationResponse;

import io.netty.handler.codec.http2.Http2Exception;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public interface INettyHttp2Connection<T extends IApnsPushNotification> {

	void abortConnection(ErrorResponse errorResponse) throws Http2Exception;

	Future<Void> connect(String host);

	Future<Void> connectSandBox();

	Future<Void> connectProduction();

	Future<Void> connect(String host, int port);

	boolean isConnected();

	void setConnectionTimeout(int timeoutMillis);

	void waitForInitialSettings() throws InterruptedException;

	Future<Void> disconnect();

	Future<IApnsPushNotificationResponse<T>> sendNotification(T notification);

	Map<T, Promise<IApnsPushNotificationResponse<T>>> getResponsePromisesMap();
	
	int getStreamId();
}
