package com.zaly.push.pns;

import com.zaly.push.constant.AppEnum;

public interface IPushNotification {

	void send(AppEnum app, IPushPackage pack);

}
