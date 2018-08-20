package com.zaly.push.constant;

public enum ErrorCode {

	SUCCESS("success", ""),

	ERROR("error", "");

	private String code;
	private String info;

	ErrorCode(String code, String info) {
		this.code = code;
		this.info = info;
	}

	public String getCode() {
		return this.code;
	}

	public String getInfo() {
		return this.info;
	}

}
