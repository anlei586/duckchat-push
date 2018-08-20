package com.zaly.push.apns.netty;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaly.push.apns.constant.ApnsHttp2Properties;
import com.zaly.push.apns.notification.ApnsHttp2PushNotificationResponse;
import com.zaly.push.apns.notification.IApnsPushNotification;
import com.zaly.push.apns.utils.DateAsMillisecondsSinceEpochTypeAdapter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.PromiseCombiner;
import io.netty.util.concurrent.ScheduledFuture;

/**
 * @author frank@linkedkeeper.com on 2016/12/28.
 */
class NettyHttp2APNsHandler<T extends IApnsPushNotification> extends Http2ConnectionHandler {
	private static final Logger logger = LoggerFactory.getLogger(NettyHttp2APNsHandler.class);

	private final AtomicBoolean receivedInitialSettings = new AtomicBoolean(false);

	private AtomicInteger NEXT_STREAM_ID = new AtomicInteger(1);

	// 存放所有<StreamID,Header>
	private final Map<Integer, Http2Headers> headersByStreamId = new HashMap<>();
	// 存放所有<StreamID,Notification>
	private final Map<Integer, T> pushNotificationsByStreamId = new HashMap<>();

	private final NettyHttp2Connection<T> apnsHttp2Client;
	private final String authority;

	private long nextPingId = new Random().nextLong();
	private ScheduledFuture<?> pingTimeoutFuture;

	private final int maxUnflushedNotifications;
	private int unflushedNotifications = 0;

	private static final int PING_TIMEOUT_SECONDS = 20;

	private static final String APNS_PATH_PREFIX = "/3/device/";
	private static final AsciiString APNS_EXPIRATION_HEADER = new AsciiString("apns-expiration");
	private static final AsciiString APNS_TOPIC_HEADER = new AsciiString("apns-topic");
	private static final AsciiString APNS_PRIORITY_HEADER = new AsciiString("apns-priority");

	/**
	 * 限制发送APNs内容最大4kb
	 */
	private static final int INITIAL_PAYLOAD_BUFFER_CAPACITY = 4096;
	private static final long STREAM_ID_RESET_THRESHOLD = Integer.MAX_VALUE - 2;

	/**
	 * gson:from google,turn Java to Json
	 */
	private static final Gson gson = new GsonBuilder()
			.registerTypeAdapter(Date.class, new DateAsMillisecondsSinceEpochTypeAdapter()).create();

	public static class ApnsHttp2ClientHandlerBuilder<S extends IApnsPushNotification>
			extends AbstractHttp2ConnectionHandlerBuilder<NettyHttp2APNsHandler<S>, ApnsHttp2ClientHandlerBuilder<S>> {

		private NettyHttp2Connection<S> apnsHttp2Client;
		private String authority;
		private int maxUnflushedNotifications = 0;

		public ApnsHttp2ClientHandlerBuilder<S> apnsHttp2Client(final NettyHttp2Connection<S> apnsHttp2Client) {
			this.apnsHttp2Client = apnsHttp2Client;
			return this;
		}

		public NettyHttp2Connection<S> apsnHttp2Client() {
			return this.apnsHttp2Client;
		}

		public ApnsHttp2ClientHandlerBuilder<S> authority(final String authority) {
			this.authority = authority;
			return this;
		}

		public String authority() {
			return this.authority;
		}

		public ApnsHttp2ClientHandlerBuilder<S> maxUnflushedNotifications(final int maxUnflushedNotifications) {
			this.maxUnflushedNotifications = maxUnflushedNotifications;
			return this;
		}

		public int maxUnflushedNotifications() {
			return this.maxUnflushedNotifications;
		}

		@Override
		public ApnsHttp2ClientHandlerBuilder<S> server(final boolean isServer) {
			return super.server(isServer);
		}

		@Override
		public ApnsHttp2ClientHandlerBuilder<S> encoderEnforceMaxConcurrentStreams(
				final boolean enforceMaxConcurrentStreams) {
			return super.encoderEnforceMaxConcurrentStreams(enforceMaxConcurrentStreams);
		}

		@Override
		public NettyHttp2APNsHandler<S> build(final Http2ConnectionDecoder decoder,
				final Http2ConnectionEncoder encoder, final Http2Settings initialSettings) throws Exception {
			Objects.requireNonNull(this.authority, "Authority must be set before building an HttpClientHandler.");

			final NettyHttp2APNsHandler<S> handler = new NettyHttp2APNsHandler<>(decoder, encoder, initialSettings,
					this.apnsHttp2Client, this.authority, this.maxUnflushedNotifications);
			this.frameListener(handler.new ApnsHttp2ClientHandlerFrameAdapter());
			return handler;
		}

		@Override
		public NettyHttp2APNsHandler<S> build() {
			return super.build();
		}
	}

	/**
	 * MPNs <--- APNs 负责读取APNs发送的类容 <br>
	 * Http2通信，内容就是收发Http2Frame<br>
	 */
	private class ApnsHttp2ClientHandlerFrameAdapter extends Http2FrameAdapter {
		/**
		 * 获取APNs服务端设置参数
		 */
		@Override
		public void onSettingsRead(final ChannelHandlerContext context, final Http2Settings settings) {
			// LogUtils.info(logger, "Received settings from APNs gateway: {0}",
			// settings);

			synchronized (NettyHttp2APNsHandler.this.receivedInitialSettings) {
				NettyHttp2APNsHandler.this.receivedInitialSettings.set(true);
				NettyHttp2APNsHandler.this.receivedInitialSettings.notifyAll();
			}
		}

		@Override
		public int onDataRead(final ChannelHandlerContext context, final int streamId, final ByteBuf data,
				final int padding, final boolean endOfStream) throws Http2Exception {
			final int bytesProcessed = data.readableBytes() + padding;

			if (endOfStream) {
				final Http2Headers headers = NettyHttp2APNsHandler.this.headersByStreamId.remove(streamId);
				final T pushNotification = NettyHttp2APNsHandler.this.pushNotificationsByStreamId.remove(streamId);

				final boolean success = HttpResponseStatus.OK.equals(HttpResponseStatus.parseLine(headers.status()));
				final ErrorResponse errorResponse = gson.fromJson(data.toString(StandardCharsets.UTF_8),
						ErrorResponse.class);

				NettyHttp2APNsHandler.this.apnsHttp2Client
						.handlePushNotificationResponse(new ApnsHttp2PushNotificationResponse<>(pushNotification,
								success, errorResponse.getReason(), errorResponse.getTimestamp()));
			} else {
				logger.error("Gateway sent a DATA frame that was not the end of a stream.");
			}

			return bytesProcessed;
		}

		@Override
		public void onHeadersRead(final ChannelHandlerContext context, final int streamId, final Http2Headers headers,
				final int streamDependency, final short weight, final boolean exclusive, final int padding,
				final boolean endOfStream) throws Http2Exception {
			this.onHeadersRead(context, streamId, headers, padding, endOfStream);
		}

		@Override
		public void onHeadersRead(final ChannelHandlerContext context, final int streamId, final Http2Headers headers,
				final int padding, final boolean endOfStream) throws Http2Exception {
			// LogUtils.info(logger, "Received headers from APNs gateway on
			// stream {0}: {1}", streamId, headers);

			final boolean success = HttpResponseStatus.OK.equals(HttpResponseStatus.parseLine(headers.status()));

			if (endOfStream) {
				if (!success) {
					logger.error("Gateway sent an end-of-stream HEADERS frame for an unsuccessful notification.");
				}
				final T pushNotification = NettyHttp2APNsHandler.this.pushNotificationsByStreamId.remove(streamId);
				NettyHttp2APNsHandler.this.apnsHttp2Client.handlePushNotificationResponse(
						new ApnsHttp2PushNotificationResponse<>(pushNotification, success, null, null));
			} else {
				NettyHttp2APNsHandler.this.headersByStreamId.put(streamId, headers);
			}
		}

		/**
		 * APNs返回ACK
		 */
		@Override
//		public void onPingAckRead(final ChannelHandlerContext context, final ByteBuf data) {
		public void onPingAckRead(final ChannelHandlerContext context, final long data) {	
			if (NettyHttp2APNsHandler.this.pingTimeoutFuture != null) {
				NettyHttp2APNsHandler.this.pingTimeoutFuture.cancel(false);
			} else {
				logger.error("Received PING ACK, but no corresponding outbound PING found.");
			}
		}

		/**
		 * 获取关闭命令
		 */
		@Override
		public void onGoAwayRead(final ChannelHandlerContext context, final int lastStreamId, final long errorCode,
				final ByteBuf debugData) throws Http2Exception {
			logger.info("Received GoAway from APNs server: {}", debugData.toString(StandardCharsets.UTF_8));

			ErrorResponse errorResponse = gson.fromJson(debugData.toString(StandardCharsets.UTF_8),
					ErrorResponse.class);
			NettyHttp2APNsHandler.this.apnsHttp2Client.abortConnection(errorResponse);
		}
	}

	protected NettyHttp2APNsHandler(final Http2ConnectionDecoder decoder, final Http2ConnectionEncoder encoder,
			final Http2Settings initialSettings, final NettyHttp2Connection<T> apnsHttp2Client, final String authority,
			final int maxUnflushedNotifications) {
		super(decoder, encoder, initialSettings);

		this.apnsHttp2Client = apnsHttp2Client;
		this.authority = authority;
		this.maxUnflushedNotifications = maxUnflushedNotifications;
	}

	/**
	 * MPNs --> APNs
	 */
	@Override
	public void write(final ChannelHandlerContext context, final Object payload, final ChannelPromise writePromise)
			throws Http2Exception {
		try {
			final T pushNotification = (T) payload;
			final int streamId = NEXT_STREAM_ID.get();

			final Http2Headers headers = new DefaultHttp2Headers().method(HttpMethod.POST.asciiName())
					.authority(this.authority).path(APNS_PATH_PREFIX + pushNotification.getToken())
					.addInt(APNS_EXPIRATION_HEADER, pushNotification.getExpiration() == null ? 0
							: (int) (pushNotification.getExpiration().getTime() / 1000));

			if (pushNotification.getPriority() != null) {
				headers.addInt(APNS_PRIORITY_HEADER, pushNotification.getPriority().getCode());
			}
			if (pushNotification.getTopic() != null) {
				headers.add(APNS_TOPIC_HEADER, pushNotification.getTopic());
			}

			final ChannelPromise headersPromise = context.newPromise();
			this.encoder().writeHeaders(context, streamId, headers, 0, false, headersPromise);
			// logger.trace("Wrote headers on stream {}: {}", streamId,
			// headers);

			final ByteBuf payloadBuffer = context.alloc().ioBuffer(INITIAL_PAYLOAD_BUFFER_CAPACITY);
			payloadBuffer.writeBytes(pushNotification.getPayload().getBytes(StandardCharsets.UTF_8));

			final ChannelPromise dataPromise = context.newPromise();
			this.encoder().writeData(context, streamId, payloadBuffer, 0, true, dataPromise);

			final PromiseCombiner promiseCombiner = new PromiseCombiner();
			promiseCombiner.addAll(headersPromise, dataPromise);
			promiseCombiner.finish(writePromise);

			writePromise.addListener(new GenericFutureListener<ChannelPromise>() {
				@Override
				public void operationComplete(final ChannelPromise future) throws Exception {
					if (future.isSuccess()) {
						NettyHttp2APNsHandler.this.pushNotificationsByStreamId.put(streamId, pushNotification);
					} else {
						logger.error("Failed to write push notification on stream {},{}.", streamId, future.cause());
					}
				}
			});

			NEXT_STREAM_ID.addAndGet(2);

			if (++this.unflushedNotifications >= this.maxUnflushedNotifications) {
				this.flush(context);
			}
			if (NEXT_STREAM_ID.get() >= STREAM_ID_RESET_THRESHOLD) {
				// context.close();
				NEXT_STREAM_ID.set(1);
			}
		} catch (final ClassCastException e) {
			logger.error("Unexpected object in pipeline: {}", payload);
			context.write(payload, writePromise);
		}
	}

	@Override
	// public void flush(final ChannelHandlerContext context) throws Http2Exception
	// {
	public void flush(final ChannelHandlerContext context) {
		super.flush(context);
		this.unflushedNotifications = 0;
	}

	/**
	 * heart - beat
	 */
	@Override
	public void userEventTriggered(final ChannelHandlerContext context, final Object event) throws Exception {
		if (event instanceof IdleStateEvent) {
			final IdleStateEvent idleStateEvent = (IdleStateEvent) event;

			if (IdleState.WRITER_IDLE.equals(idleStateEvent.state())) {
				if (this.unflushedNotifications > 0) {
					this.flush(context);
				}
			} else {
				assert PING_TIMEOUT_SECONDS < ApnsHttp2Properties.PING_IDLE_TIME_MILLIS;

				// final ByteBuf pingDataBuffer = context.alloc().ioBuffer(8, 8);
				// pingDataBuffer.writeLong(this.nextPingId++);

				// this.encoder().writePing(context, false, pingDataBuffer,
				this.encoder().writePing(context, false, this.nextPingId++,
						context.newPromise().addListener(new GenericFutureListener<ChannelFuture>() {
							@Override
							public void operationComplete(final ChannelFuture future) throws Exception {
								if (future.isSuccess()) {
									NettyHttp2APNsHandler.this.pingTimeoutFuture = future.channel().eventLoop()
											.schedule(new Runnable() {
												@Override
												public void run() {
													logger.info("Closing channel due to ping timeout.");
													future.channel().close();
												}
											}, PING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
								} else {
									logger.error("Failed to write PING frame.", future.cause());
									future.channel().close();
								}
							}
						}));
				this.flush(context);
			}
		}
		super.userEventTriggered(context, event);
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext context, final Throwable cause) throws Exception {
		if (cause instanceof WriteTimeoutException) {
			logger.error("Closing connection due to write timeout.");
			context.close();
		} else {
			logger.error("APNs client pipeline exception.cause={0}", cause);
		}
	}

	void waitForInitialSettings() throws InterruptedException {
		synchronized (this.receivedInitialSettings) {
			while (!this.receivedInitialSettings.get()) {
				this.receivedInitialSettings.wait();
			}
		}
	}

	public int getCurrentStreamId() {
		return NEXT_STREAM_ID.get();
	}
}
