package com.zaly.push.apns.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaly.push.apns.constant.ApnsHttp2Config;
import com.zaly.push.apns.netty.NettyConnectionPool;
import com.zaly.push.apns.netty.NettyHttp2Connection;
import com.zaly.push.apns.netty.api.INettyConnectionPool;
import com.zaly.push.apns.netty.api.INettyHttp2Connection;
import com.zaly.push.apns.notification.ApnsHttp2PushNotification;
import com.zaly.push.apns.notification.IApnsPushNotification;
import com.zaly.push.apns.notification.IApnsPushNotificationResponse;
import com.zaly.push.apns.utils.APNsTokenPattern;
import com.zaly.push.apns.utils.P12Utils;

import io.netty.util.concurrent.Future;

/**
 * @author frank@linkedkeeper.com on 2016/12/27.
 */
public class ApnsHttp2Client implements IApnsHttp2Client {
	private static final Logger logger = LoggerFactory.getLogger(ApnsHttp2Client.class);

	/**
	 * 直接使用netty建立单个连接
	 */
	private INettyHttp2Connection<IApnsPushNotification> nettyApnsHttp2Client;
	/**
	 * 使用连接池建立多个长连接
	 */
	private INettyConnectionPool<IApnsPushNotification> nettyConnectionPool;
	private boolean sandboxEnvironment = false;

	public ApnsHttp2Client(final File certificateFile, final String password, boolean sandboxEnvironment)
			throws SSLException, InterruptedException {
		this.sandboxEnvironment = sandboxEnvironment;
		try {
			this.nettyApnsHttp2Client = new NettyHttp2Connection<>(certificateFile, password);
			if (!this.nettyApnsHttp2Client.isConnected()) {
				logger.info("Start to link APNs http2 client connection ...");
				stablishConnection();
			}
		} catch (Exception e) {
			logger.error("ApnsHttp2 Init failure!", e);
		}

	}

	public ApnsHttp2Client(final InputStream p12InputStream, final String password, boolean sandboxEnvironment)
			throws SSLException, InterruptedException {
		this.sandboxEnvironment = sandboxEnvironment;
		try {
			KeyStore keyStore = P12Utils.loadPCKS12KeyStore(p12InputStream, password);
			this.nettyApnsHttp2Client = new NettyHttp2Connection<>(keyStore, password);
			if (!this.nettyApnsHttp2Client.isConnected()) {
				logger.error("Start to link APNs http2 client connection ...");
				stablishConnection();
			}
		} catch (Exception e) {
			logger.error("ApnsHttp2 Init failure by InputStream!", e);
		}

	}

	public ApnsHttp2Client(ApnsHttp2Config apnsHttp2Config)
			throws InterruptedException, KeyStoreException, IOException {
		this.nettyConnectionPool = NettyConnectionPool.createClientPool(apnsHttp2Config);
	}

	@Override
	public IApnsPushNotificationResponse<IApnsPushNotification> pushMessageSync(final String token,
			final String payload, final int timeout) throws Exception {
		Entry<String, INettyHttp2Connection<IApnsPushNotification>> clientEntry = null;
		try {
			clientEntry = this.nettyConnectionPool.acquire();
			if (clientEntry != null) {
				INettyHttp2Connection<IApnsPushNotification> apnsHttp2Client = clientEntry.getValue();
				final IApnsPushNotification apnsPushNotification = new ApnsHttp2PushNotification(token, null, payload);
				final Future<IApnsPushNotificationResponse<IApnsPushNotification>> sendNotificationFuture = apnsHttp2Client
						.sendNotification(apnsPushNotification);
				final IApnsPushNotificationResponse<IApnsPushNotification> apnsPushNotificationResponse = sendNotificationFuture
						.get(timeout, TimeUnit.SECONDS);
				return apnsPushNotificationResponse;
			}
		} catch (Exception e) {
			logger.error("Failed to send push (sync) notification.", e);
		} finally {
			this.nettyConnectionPool.release(clientEntry);
		}
		return null;
	}

	@Override
	public Future<IApnsPushNotificationResponse<IApnsPushNotification>> pushMessageAsync(final String token,
			final String payload) throws ExecutionException {
		Entry<String, INettyHttp2Connection<IApnsPushNotification>> clientEntry = null;
		try {
			clientEntry = this.nettyConnectionPool.acquire();
			if (clientEntry != null) {
				INettyHttp2Connection<IApnsPushNotification> apnsHttp2Client = clientEntry.getValue();
				final IApnsPushNotification apnsPushNotification = new ApnsHttp2PushNotification(
						APNsTokenPattern.formatToken(token), null, payload);
				final Future<IApnsPushNotificationResponse<IApnsPushNotification>> future = apnsHttp2Client
						.sendNotification(apnsPushNotification);
				return future;
			}
		} catch (Exception e) {
			logger.error("Failed to send push (async) notification", e);
		} finally {
			this.nettyConnectionPool.release(clientEntry);
		}
		return null;
	}

	@Override
	public void disconnect() throws InterruptedException, IOException {
		this.nettyConnectionPool.close();
	}

	private void stablishConnection() throws InterruptedException {
		final Future<Void> connectFuture = sandboxEnvironment ? this.nettyApnsHttp2Client.connectSandBox()
				: this.nettyApnsHttp2Client.connectProduction();
		connectFuture.await();
	}
}