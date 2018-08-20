package com.zaly.push.apns.netty.bean;

public class ConnStateBean {
	private String connName;
	private int streamId;
	private String connState;
	private int responseSize;

	public String getConnName() {
		return connName;
	}

	public void setConnName(String connName) {
		this.connName = connName;
	}

	public int getStreamId() {
		return streamId;
	}

	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

	public String getConnState() {
		return connState;
	}

	public void setConnState(String connState) {
		this.connState = connState;
	}

	public int getResponseSize() {
		return responseSize;
	}

	public void setResponseSize(int responseSize) {
		this.responseSize = responseSize;
	}

}
