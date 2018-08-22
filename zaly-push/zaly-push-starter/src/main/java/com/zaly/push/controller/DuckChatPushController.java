package com.zaly.push.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.protobuf.Any;
import com.zaly.proto.core.Net;
import com.zaly.proto.platform.ApiPushPayload;
import com.zaly.proto.platform.Common;
import com.zaly.push.constant.AppEnum;
import com.zaly.push.constant.ErrorCode;
import com.zaly.push.pns.AsyncTask;
import com.zaly.push.pns.IPushPackage;
import com.zaly.push.utils.PnsUtils;

@Controller
@RequestMapping("/duckchat")
public class DuckChatPushController extends AbstractPushController {
	private final Logger logger = LoggerFactory.getLogger("/duckchat/../");

	@RequestMapping(method = RequestMethod.POST, value = "/push")
	@ResponseBody
	public byte[] sendNotification(HttpServletRequest httpRequest, @RequestBody byte[] bodyBase64Bytes) {
		ErrorCode errCode = ErrorCode.ERROR;
		String action = "api.push.payload";
		try {

			Net.TransportData data = Net.TransportData.parseFrom(bodyBase64Bytes);
			action = data.getAction();
			Any any = data.getBody();

			ApiPushPayload.ApiPushPayloadRequest request = any.unpack(ApiPushPayload.ApiPushPayloadRequest.class);

			List<Common.Payload> payloadList = request.getPayloadsList();

			logger.info("/duckchat/push count={} payloadList={}", payloadList.size(), payloadList.toString());

			for (Common.Payload payload : payloadList) {

				logger.info("/duckchat/push payload={}", payload.toString());

				Common.PayloadType type = payload.getType();

				IPushPackage pack = null;

				switch (type) {
				case UNRECOGNIZED:
					throw new Exception("UNRECOGNIZED TYPE");
				case UNKNOW_TYPE:
					throw new Exception("UNKNOW_TYPE");
				case IOS:
					pack = PnsUtils.buidApns(payload);
					break;
				case ANDROID:
					pack = PnsUtils.buidUMeng(payload);
					break;
				case XIAOMI:
					pack = PnsUtils.buidXiaomi(payload);
					break;
				case HUAWEI:
					pack = PnsUtils.buidHuawei(payload);
					break;
				default:

					break;
				}

				AsyncTask.execute(AppEnum.DUCKCHAT, pack);
			}

			errCode = ErrorCode.SUCCESS;
		} catch (Exception e) {
			e.printStackTrace();
			// logger.error("/duckchat/push error", e);
		}

		ApiPushPayload.ApiPushPayloadResponse response = ApiPushPayload.ApiPushPayloadResponse.newBuilder().build();
		return packageToData(action, response, errCode);
	}

}
