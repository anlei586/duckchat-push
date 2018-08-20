package com.zaly.push.pns.xiaomi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xiaomi.xmpush.server.Constants;
import com.xiaomi.xmpush.server.Message;
import com.xiaomi.xmpush.server.Result;
import com.xiaomi.xmpush.server.Sender;

/**
 * 发送xiaomiPush的客户端管理
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-05-04 18:41:48
 */
public class XiaomiPushClient {
	private static final Logger logger = LoggerFactory.getLogger(XiaomiPushClient.class);

	public static Result pushMessage(String appSecretKey, String token, Message message) throws Exception {
		logger.info("push xiaoxi message appSecretKey={} token={} message={}", appSecretKey, token, message);
		Sender sender = null;
		Constants.useOfficial(); // Constants.useSandbox()->only for ios sandbox
		sender = new Sender(appSecretKey);
		Result result = sender.send(message, token, 1); // 根据regID，发送消息到指定设备上，不重试。
		return result;
	}

	/**
	 * 批量发送模式
	 * 
	 * @throws Exception
	 */
	public static void pushTargetedMessage(Message mesage) throws Exception {
		// Constants.useOfficial();
		// Sender sender = new Sender(APP_SECRET_KEY);
		// List<TargetedMessage> messages = new ArrayList<TargetedMessage>();
		// TargetedMessage targetedMessage1 = new TargetedMessage();
		// targetedMessage1.setTarget(TargetedMessage.TARGET_TYPE_ALIAS, "alias1");
		// String messagePayload1 = "This is a message1";
		// String title1 = "notification title1";
		// String description1 = "notification description1";
		// Message message1 = new
		// Message.Builder().title(title1).description(description1).payload(messagePayload1)
		// .restrictedPackageName(MY_PACKAGE_NAME).notifyType(1) // 使用默认提示音提示
		// .build();
		// targetedMessage1.setMessage(message1);
		// messages.add(targetedMessage1);
		//
		//
		// sender.send(messages, 0); // 根据alias，发送消息到指定设备上，不重试。
	}

}
