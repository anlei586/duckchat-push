package com.zaly.push.apns.constant;

public class ApnsHttp2Properties {
	public static final long DEFAULT_WRITE_TIMEOUT_MILLIS = 20_000;
	public static final long DEFAULT_FLUSH_AFTER_IDLE_MILLIS = 50;
	public static final int DEFAULT_MAX_UNFLUSHED_NOTIFICATIONS = 1280;

	public static final String PRODUCTION_APNS_HOST = "api.push.apple.com";
	public static final String DEVELOPMENT_APNS_HOST = "api.development.push.apple.com";

	public static final int DEFAULT_APNS_PORT = 443;
	public static final int ALTERNATE_APNS_PORT = 2197;
	public static final long INITIAL_RECONNECT_DELAY_SECONDS = 1;
	public static final long MAX_RECONNECT_DELAY_SECONDS = 60;
	public static final int PING_IDLE_TIME_MILLIS = 60_000;
}
