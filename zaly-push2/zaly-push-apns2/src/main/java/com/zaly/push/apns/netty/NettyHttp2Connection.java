package com.zaly.push.apns.netty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaly.push.apns.constant.ApnsHttp2Properties;
import com.zaly.push.apns.exceptions.ClientNotConnectedException;
import com.zaly.push.apns.netty.api.INettyHttp2Connection;
import com.zaly.push.apns.notification.IApnsPushNotification;
import com.zaly.push.apns.notification.IApnsPushNotificationResponse;
import com.zaly.push.apns.utils.P12Utils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SucceededFuture;

/**
 * @author frank@linkedkeeper.com on 2016/12/27.
 */
public class NettyHttp2Connection<T extends IApnsPushNotification> implements INettyHttp2Connection<T> {
	private static final Logger logger = LoggerFactory.getLogger(NettyHttp2Connection.class);

	private static final String EPOLL_EVENT_LOOP_GROUP_CLASS = "io.netty.channel.epoll.EpollEventLoopGroup";
	private static final String EPOLL_SOCKET_CHANNEL_CLASS = "io.netty.channel.epoll.EpollSocketChannel";

	private final Bootstrap bootstrap;
	private final boolean shouldShutDownEventLoopGroup;

	private Long gracefulShutdownTimeoutMillis;

	private volatile ChannelPromise connectionReadyPromise;
	private volatile ChannelPromise reconnectionPromise;
	private long reconnectDelaySeconds = ApnsHttp2Properties.INITIAL_RECONNECT_DELAY_SECONDS;

	private NettyHttp2APNsHandler<T> apnsHttp2ClientHandler;

	private ArrayList<String> identities;

	private final Map<T, Promise<IApnsPushNotificationResponse<T>>> responsePromises = new IdentityHashMap<>();

	private static final ClientNotConnectedException NOT_CONNECTED_EXCEPTION = new ClientNotConnectedException();

	public NettyHttp2Connection(final File p12File, final String password) throws IOException, KeyStoreException {
		this(p12File, password, null);
	}

	public NettyHttp2Connection(final File p12File, final String password, final EventLoopGroup eventLoopGroup)
			throws IOException, KeyStoreException {
		this(NettyHttp2Connection.getSslContextWithP12File(p12File, password), eventLoopGroup);
		try (final InputStream p12InputStream = new FileInputStream(p12File)) {
			loadIdentifiers(loadKeyStore(p12InputStream, password));
		}
	}

	public NettyHttp2Connection(KeyStore keyStore, final String password) throws SSLException {
		this(keyStore, password, null);
	}

	public NettyHttp2Connection(final KeyStore keyStore, final String password, final EventLoopGroup eventLoopGroup)
			throws SSLException {
		this(NettyHttp2Connection.getSslContextWithP12InputStream(keyStore, password), eventLoopGroup);
		loadIdentifiers(keyStore);
	}

	@Override
	public void abortConnection(ErrorResponse errorResponse) throws Http2Exception {
		disconnect();
		throw new Http2Exception(Http2Error.CONNECT_ERROR, errorResponse.getReason());
	}

	private static KeyStore loadKeyStore(final InputStream p12InputStream, final String password) throws SSLException {
		try {
			return P12Utils.loadPCKS12KeyStore(p12InputStream, password);
		} catch (KeyStoreException | IOException e) {
			throw new SSLException(e);
		}
	}

	private void loadIdentifiers(KeyStore keyStore) throws SSLException {
		try {
			this.identities = P12Utils.getIdentitiesForP12File(keyStore);
		} catch (KeyStoreException | IOException e) {
			throw new SSLException(e);
		}
	}

	public NettyHttp2Connection(final X509Certificate certificate, final PrivateKey privateKey,
			final String privateKeyPassword) throws SSLException {
		this(certificate, privateKey, privateKeyPassword, null);
	}

	public NettyHttp2Connection(final X509Certificate certificate, final PrivateKey privateKey,
			final String privateKeyPassword, final EventLoopGroup eventLoopGroup) throws SSLException {
		this(NettyHttp2Connection.getSslContextWithCertificateAndPrivateKey(certificate, privateKey,
				privateKeyPassword), eventLoopGroup);
	}

	private static SslContext getSslContextWithP12File(final File p12File, final String password)
			throws IOException, KeyStoreException {
		try (final InputStream p12InputStream = new FileInputStream(p12File)) {
			return NettyHttp2Connection.getSslContextWithP12InputStream(loadKeyStore(p12InputStream, password),
					password);
		}
	}

	private static SslContext getSslContextWithP12InputStream(final KeyStore keyStore, final String password)
			throws SSLException {
		final X509Certificate x509Certificate;
		final PrivateKey privateKey;
		try {
			final PrivateKeyEntry privateKeyEntry = P12Utils.getFirstPrivateKeyEntryFromP12InputStream(keyStore,
					password);
			final Certificate certificate = privateKeyEntry.getCertificate();
			if (!(certificate instanceof X509Certificate)) {
				throw new KeyStoreException(
						"Found a certificate in the provided PKCS#12 file, but it was not an X.509 certificate.");
			}
			x509Certificate = (X509Certificate) certificate;
			privateKey = privateKeyEntry.getPrivateKey();
		} catch (final KeyStoreException | IOException e) {
			throw new SSLException(e);
		}
		return NettyHttp2Connection.getSslContextWithCertificateAndPrivateKey(x509Certificate, privateKey, password);
	}

	private static SslContext getSslContextWithCertificateAndPrivateKey(final X509Certificate certificate,
			final PrivateKey privateKey, final String privateKeyPassword) throws SSLException {
		return NettyHttp2Connection.getBaseSslContextBuilder().keyManager(privateKey, privateKeyPassword, certificate)
				.build();
	}

	private static SslContextBuilder getBaseSslContextBuilder() {
		final SslProvider sslProvider;

		if (OpenSsl.isAvailable()) {
			if (OpenSsl.isAlpnSupported()) {
				sslProvider = SslProvider.OPENSSL;
			} else {
				sslProvider = SslProvider.JDK;
			}
		} else {
			sslProvider = SslProvider.JDK;
		}

		return SslContextBuilder.forClient().sslProvider(sslProvider)
				.ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE).applicationProtocolConfig(
						new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
								SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2));
	}

	/**
	 * MPNs和APNs建立连接分为两步 <br>
	 * 第一步：构造函数中初始化，Netty建立Http2长连接的配置项
	 *
	 * @param sslContext
	 * @param eventLoopGroup
	 */
	protected NettyHttp2Connection(final SslContext sslContext, final EventLoopGroup eventLoopGroup) {
		this.bootstrap = new Bootstrap();

		/**
		 * 客户端EventLoopGroup：注册Channel，并管理其胜利周期<br>
		 * 这默认Netty中的 NioEventLoopGroup
		 */
		if (eventLoopGroup != null) {
			this.bootstrap.group(eventLoopGroup);
			this.shouldShutDownEventLoopGroup = false;
		} else {
			this.bootstrap.group(new NioEventLoopGroup(1));
			this.shouldShutDownEventLoopGroup = true;
		}
		/**
		 * 是否可以配置多个channel
		 */
		this.bootstrap.channel(this.getSocketChannelClass(this.bootstrap.config().group()));
		this.bootstrap.option(ChannelOption.TCP_NODELAY, true);
		this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		/**
		 * 批量处理多个Handler业务逻辑
		 */
		this.bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(final SocketChannel channel) throws Exception {
				final ChannelPipeline pipeline = channel.pipeline();

				if (ApnsHttp2Properties.DEFAULT_WRITE_TIMEOUT_MILLIS > 0) {
					pipeline.addLast(new WriteTimeoutHandler(ApnsHttp2Properties.DEFAULT_WRITE_TIMEOUT_MILLIS,
							TimeUnit.MILLISECONDS));
				}

				pipeline.addLast(sslContext.newHandler(channel.alloc()));
				/**
				 * ApplicationProtocolNegotiationHandler
				 */
				pipeline.addLast(new ApplicationProtocolNegotiationHandler("MPNs-fallbackProtocol") {
					@Override
					protected void configurePipeline(final ChannelHandlerContext context, final String protocol) {
						if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
							/**
							 * APNs-NettyHttp2Handler
							 */
							apnsHttp2ClientHandler = new NettyHttp2APNsHandler.ApnsHttp2ClientHandlerBuilder<T>()
									.server(false).apnsHttp2Client(NettyHttp2Connection.this)
									.authority(((InetSocketAddress) context.channel().remoteAddress()).getHostName())
									.maxUnflushedNotifications(ApnsHttp2Properties.DEFAULT_MAX_UNFLUSHED_NOTIFICATIONS)
									.encoderEnforceMaxConcurrentStreams(true).build();

							synchronized (NettyHttp2Connection.this.bootstrap) {
								if (NettyHttp2Connection.this.gracefulShutdownTimeoutMillis != null) {
									apnsHttp2ClientHandler.gracefulShutdownTimeoutMillis(
											NettyHttp2Connection.this.gracefulShutdownTimeoutMillis);
								}
							}

							/**
							 * IdleStateHandler 负责与APNs发送心跳 <br>
							 * IdleStateHandler is send heart-beat to apns that remain the connection <br>
							 **/
							context.pipeline().addLast(
									new IdleStateHandler(0, ApnsHttp2Properties.DEFAULT_FLUSH_AFTER_IDLE_MILLIS,
											ApnsHttp2Properties.PING_IDLE_TIME_MILLIS, TimeUnit.MILLISECONDS));
							context.pipeline().addLast(apnsHttp2ClientHandler);

							context.channel().eventLoop().submit(new Runnable() {
								@Override
								public void run() {
									final ChannelPromise connectionReadyPromise = NettyHttp2Connection.this.connectionReadyPromise;
									if (connectionReadyPromise != null) {
										connectionReadyPromise.trySuccess();
									}
								}
							});
						} else {
							logger.error("Unexpected protocol: {}", protocol);
							context.close();
						}
					}

					/**
					 * 与APNs建立Http2连接，握手失败处理
					 */
					@Override
					protected void handshakeFailure(final ChannelHandlerContext context, final Throwable cause)
							throws Exception {
						final ChannelPromise connectionReadyPromise = NettyHttp2Connection.this.connectionReadyPromise;
						if (connectionReadyPromise != null) {
							connectionReadyPromise.tryFailure(cause);
						}
						super.handshakeFailure(context, cause);
					}
				});
			}
		});
	}

	private Class<? extends Channel> getSocketChannelClass(final EventLoopGroup eventLoopGroup) {
		if (eventLoopGroup == null) {
			logger.warn(
					"Asked for socket channel class to work with null event loop group, returning NioSocketChannel class.");
			return NioSocketChannel.class;
		}
		if (eventLoopGroup instanceof NioEventLoopGroup) {
			return NioSocketChannel.class;
		} else if (eventLoopGroup instanceof OioEventLoopGroup) {
			return OioSocketChannel.class;
		}
		final String className = eventLoopGroup.getClass().getName();
		if (EPOLL_EVENT_LOOP_GROUP_CLASS.equals(className)) {
			return this.loadSocketChannelClass(EPOLL_SOCKET_CHANNEL_CLASS);
		}

		throw new IllegalArgumentException(
				"Don't know which socket channel class to return for event loop group " + className);
	}

	private Class<? extends Channel> loadSocketChannelClass(final String className) {
		try {
			final Class<?> clazz = Class.forName(className);
			logger.info("Loaded socket channel class: {0}", clazz);
			return clazz.asSubclass(Channel.class);
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	@Override
	public void setConnectionTimeout(final int timeoutMillis) {
		synchronized (this.bootstrap) {
			this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis);
		}
	}

	/**
	 * 进行连接APNs操作：指定Host+默认APNs端口
	 */
	@Override
	public Future<Void> connect(final String host) {
		return this.connect(host, ApnsHttp2Properties.DEFAULT_APNS_PORT);
	}

	/**
	 * 测试／沙盒／开发环境：默认APNs端口
	 */
	@Override
	public Future<Void> connectSandBox() {
		return this.connect(ApnsHttp2Properties.DEVELOPMENT_APNS_HOST, ApnsHttp2Properties.DEFAULT_APNS_PORT);
	}

	/**
	 * 正式／线上／生产环境：默认APNs端口
	 */
	@Override
	public Future<Void> connectProduction() {
		return this.connect(ApnsHttp2Properties.PRODUCTION_APNS_HOST, ApnsHttp2Properties.DEFAULT_APNS_PORT);
	}

	/**
	 * 第二步:当客户端发起Connect请求，连接器通过当前设置配置项，建立Http2连接<br>
	 * 
	 * 注意：这里将设置配置项 AND 建立连接两个操作步骤区分开 <br>
	 * 
	 * Future && Promise 获取移步操作的结果 <br>
	 * Netty中的Future继承JDK中的java.util.concurrent.Future <br>
	 * 
	 */
	@Override
	public Future<Void> connect(final String host, final int port) {
		final Future<Void> connectionReadyFuture;

		if (this.bootstrap.config().group().isShuttingDown() || this.bootstrap.config().group().isShutdown()) {
			connectionReadyFuture = new FailedFuture<>(GlobalEventExecutor.INSTANCE,
					new IllegalStateException("Client's event loop group has been shut down and cannot be restarted."));
		} else {
			synchronized (this.bootstrap) {

				logger.info("connect {}:{}. start this APNs connectionReadyPromise={}.", host, port,
						this.connectionReadyPromise);

				if (this.connectionReadyPromise == null) {
					final ChannelFuture connectFuture = this.bootstrap.connect(host, port);
					this.connectionReadyPromise = connectFuture.channel().newPromise();
					/**
					 * 给每个Channel设置一个监听器，当channel关闭时候进行重连尝试.( this listener is add in channel, its
					 * effect is attempt to reconnect when the channel close)
					 */
					connectFuture.channel().closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
						@Override
						public void operationComplete(final ChannelFuture future) throws Exception {
							synchronized (NettyHttp2Connection.this.bootstrap) {
								if (NettyHttp2Connection.this.connectionReadyPromise != null) {
									NettyHttp2Connection.this.connectionReadyPromise
											.tryFailure(new IllegalStateException(
													"Channel closed before HTTP/2 preface completed."));
									NettyHttp2Connection.this.connectionReadyPromise = null;
								}
								if (NettyHttp2Connection.this.reconnectionPromise != null) {
									logger.info("Disconnected. Next automatic reconnection attempt in {} seconds.",
											NettyHttp2Connection.this.reconnectDelaySeconds);
									future.channel().eventLoop().schedule(new Runnable() {
										@Override
										public void run() {
											logger.warn("Attempting to reconnect.");
											NettyHttp2Connection.this.connect(host, port);
										}
									}, NettyHttp2Connection.this.reconnectDelaySeconds, TimeUnit.SECONDS);
									NettyHttp2Connection.this.reconnectDelaySeconds = Math.min(
											NettyHttp2Connection.this.reconnectDelaySeconds,
											ApnsHttp2Properties.MAX_RECONNECT_DELAY_SECONDS);
								}
							}
							future.channel().eventLoop().submit(new Runnable() {
								@Override
								public void run() {
									for (final Promise<IApnsPushNotificationResponse<T>> responsePromise : NettyHttp2Connection.this.responsePromises
											.values()) {
										responsePromise.tryFailure(
												new ClientNotConnectedException("Client disconnected unexpectedly."));
									}
									NettyHttp2Connection.this.responsePromises.clear();
								}
							});
						}
					});

					/**
					 * this listener's effect is get reconnectionPromise after connect success.
					 * 
					 * 获取移步结果的两种方式：<br>
					 * 1.异步调用，同时等待移步结果 <br>
					 * 2.异步调用，同时设置监听，当返回返回值触发操作 <br>
					 * 
					 * 这里使用第二种监听方式。
					 **/
					this.connectionReadyPromise.addListener(new GenericFutureListener<ChannelFuture>() {
						@Override
						public void operationComplete(final ChannelFuture future) throws Exception {
							if (future.isSuccess()) {
								synchronized (NettyHttp2Connection.this.bootstrap) {
									if (NettyHttp2Connection.this.reconnectionPromise != null) {
										logger.info("Connection to {} restored.", future.channel().remoteAddress());
										NettyHttp2Connection.this.reconnectionPromise.trySuccess();
									} else {
										logger.info("Connected to {}.", future.channel().remoteAddress());
									}
									NettyHttp2Connection.this.reconnectDelaySeconds = ApnsHttp2Properties.INITIAL_RECONNECT_DELAY_SECONDS;
									NettyHttp2Connection.this.reconnectionPromise = future.channel().newPromise();
								}
							} else {
								logger.error("Failed to connect e={0}", future.cause());
							}
						}
					});
				}

				/**
				 * APNs-Http长连接成功
				 */
				if (this.connectionReadyPromise != null) {
					logger.info("Finish this APNs connect isSuccess={} AND its channel isActive={}",
							this.connectionReadyPromise.isSuccess(), this.connectionReadyPromise.channel().isActive());
				}
				connectionReadyFuture = this.connectionReadyPromise;
			}
		}
		return connectionReadyFuture;
	}

	@Override
	public boolean isConnected() {
		final ChannelPromise connectionReadyPromise = this.connectionReadyPromise;
		return (connectionReadyPromise != null && connectionReadyPromise.isSuccess());
	}

	@Override
	public void waitForInitialSettings() throws InterruptedException {
		this.connectionReadyPromise.channel().pipeline().get(NettyHttp2APNsHandler.class).waitForInitialSettings();
	}

	public Future<Void> getReconnectionFuture() {
		final Future<Void> reconnectionFuture;
		synchronized (this.bootstrap) {
			if (this.isConnected()) {
				reconnectionFuture = this.connectionReadyPromise.channel().newSucceededFuture();
			} else if (this.reconnectionPromise != null) {
				reconnectionFuture = this.reconnectionPromise;
			} else {
				reconnectionFuture = new FailedFuture<>(GlobalEventExecutor.INSTANCE,
						new IllegalStateException("Client was not previously connected."));
			}
		}
		return reconnectionFuture;
	}

	@Override
	public Future<IApnsPushNotificationResponse<T>> sendNotification(final T notification) {
		final Future<IApnsPushNotificationResponse<T>> responseFuture;

		if (connectionReadyPromise != null && connectionReadyPromise.isSuccess()
				&& connectionReadyPromise.channel().isActive()) {
			verifyTopic(notification);

			final ChannelPromise connectionReadyPromise = this.connectionReadyPromise;
			final DefaultPromise<IApnsPushNotificationResponse<T>> responsePromise = new DefaultPromise<>(
					connectionReadyPromise.channel().eventLoop());

			connectionReadyPromise.channel().eventLoop().submit(new Runnable() {
				@Override
				public void run() {
					if (NettyHttp2Connection.this.responsePromises.containsKey(notification)) {
						responsePromise.setFailure(new IllegalStateException(
								"The given notification has already been sent and not yet resolved."));
					} else {
						NettyHttp2Connection.this.responsePromises.put(notification, responsePromise);
					}
				}
			});

			connectionReadyPromise.channel().write(notification)
					.addListener(new GenericFutureListener<ChannelFuture>() {
						@Override
						public void operationComplete(final ChannelFuture future) throws Exception {
							if (!future.isSuccess()) {
								logger.error("Failed to write push notification: {},cause={}", notification,
										future.cause());
								NettyHttp2Connection.this.responsePromises.remove(notification);
								responsePromise.tryFailure(future.cause());
							} else {
								// LogUtils.info(logger, "Success to write push
								// notification: {0}", notification);
							}
						}
					});

			responseFuture = responsePromise;
		} else {
			logger.error("Failed to send push notification because client is not connected: {}", notification);
			responseFuture = new FailedFuture<>(GlobalEventExecutor.INSTANCE, NOT_CONNECTED_EXCEPTION);
		}
		return responseFuture;
	}

	private void verifyTopic(T notification) {
		if (notification.getTopic() == null && this.identities != null && !this.identities.isEmpty()) {
			notification.setTopic(this.identities.get(0));
		}
	}

	/**
	 * 发送完成以后，将返回结果提交到这里
	 */
	protected void handlePushNotificationResponse(final IApnsPushNotificationResponse<T> response) {
		try {
			if (response.getApnsPushNotification() != null) {
				this.responsePromises.remove(response.getApnsPushNotification()).setSuccess(response);
			} else {
				this.responsePromises.clear();
			}

		} catch (Exception e) {
			logger.error("handlePushNotificationResponse error!", e);
		}
	}

	public void setGracefulShutdownTimeout(final long timeoutMillis) {
		synchronized (this.bootstrap) {
			this.gracefulShutdownTimeoutMillis = timeoutMillis;
			if (this.connectionReadyPromise != null) {
				final NettyHttp2APNsHandler handler = this.connectionReadyPromise.channel().pipeline()
						.get(NettyHttp2APNsHandler.class);
				if (handler != null) {
					handler.gracefulShutdownTimeoutMillis(timeoutMillis);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Future<Void> disconnect() {
		logger.info("close the Http2 Link, Disconnecting.");

		final Future<Void> disconnectFuture;
		synchronized (this.bootstrap) {
			this.reconnectionPromise = null;

			final Future<Void> channelCloseFuture;

			if (this.connectionReadyPromise != null) {
				channelCloseFuture = this.connectionReadyPromise.channel().close();
			} else {
				channelCloseFuture = new SucceededFuture<>(GlobalEventExecutor.INSTANCE, null);
			}

			if (this.shouldShutDownEventLoopGroup) {
				channelCloseFuture.addListener(new GenericFutureListener<Future<Void>>() {
					@Override
					public void operationComplete(final Future<Void> future) throws Exception {
						NettyHttp2Connection.this.bootstrap.config().group().shutdownGracefully();
					}
				});
				disconnectFuture = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

				this.bootstrap.config().group().terminationFuture().addListener(new GenericFutureListener() {
					@Override
					public void operationComplete(final Future future) throws Exception {
						assert disconnectFuture instanceof DefaultPromise;
						((DefaultPromise<Void>) disconnectFuture).trySuccess(null);
					}
				});
			} else {
				disconnectFuture = channelCloseFuture;
			}
		}
		return disconnectFuture;
	}

	@Override
	public Map<T, Promise<IApnsPushNotificationResponse<T>>> getResponsePromisesMap() {
		return responsePromises;
	}

	@Override
	public int getStreamId() {
		return apnsHttp2ClientHandler.getCurrentStreamId();
	}
}
