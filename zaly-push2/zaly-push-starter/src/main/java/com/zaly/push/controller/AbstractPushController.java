package com.zaly.push.controller;

import java.util.HashMap;
import java.util.Map;

import com.zaly.proto.core.Net;
import com.zaly.push.constant.ErrorCode;

public abstract class AbstractPushController {

	protected byte[] packageToData(String action, com.google.protobuf.Message message, ErrorCode errCode) {

		com.google.protobuf.Any anyBody = com.google.protobuf.Any.pack(message);

		Map<String, String> header = new HashMap<String, String>();

		if (errCode != null) {
			header.put("_" + Net.TransportDataHeaderKey.HeaderErrorCode_VALUE, errCode.getCode());
			header.put("_" + Net.TransportDataHeaderKey.HeaderErrorCode_VALUE, errCode.getInfo());
		} else {
			header.put("_" + Net.TransportDataHeaderKey.HeaderErrorCode_VALUE, ErrorCode.ERROR.getCode());
			header.put("_" + Net.TransportDataHeaderKey.HeaderErrorCode_VALUE, ErrorCode.ERROR.getInfo());
		}

		Net.TransportData data = Net.TransportData.newBuilder()// trasportdata
				.setAction(action) // action
				.putAllHeader(header) // header
				.setBody(anyBody) // body
				.build();

		return data.toByteArray();
	}

}
