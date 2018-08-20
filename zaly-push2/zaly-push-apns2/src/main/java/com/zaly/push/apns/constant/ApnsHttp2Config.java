package com.zaly.push.apns.constant;

import java.io.InputStream;

/**
 * 
 * MPNs连接APNs配置属性
 * 
 * @author Mr.an
 * @since 2017.05.23
 *
 */
public class ApnsHttp2Config {

	private String name;

	private String topic;

	private InputStream keyStoreStream;

	private String password;

	private boolean sandboxEnvironment;

	private int poolSize;

	public InputStream getKeyStoreStream() {
		return keyStoreStream;
	}

	public void setKeyStoreStream(InputStream keyStoreStream) {
		this.keyStoreStream = keyStoreStream;
	}

	public void setKeyStoreStream(String keyStoreFilePath) {
		InputStream inputStream = ApnsHttp2Config.class.getClassLoader().getResourceAsStream(keyStoreFilePath);
		if (inputStream == null) {
			throw new IllegalArgumentException("Keystore file not found. " + keyStoreFilePath);
		}
		setKeyStoreStream(inputStream);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isSandboxEnvironment() {
		return sandboxEnvironment;
	}

	public void setSandboxEnvironment(boolean sandboxEnvironment) {
		this.sandboxEnvironment = sandboxEnvironment;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

}
