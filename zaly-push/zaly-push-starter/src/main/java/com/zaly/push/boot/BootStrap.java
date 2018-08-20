package com.zaly.push.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akaxin.platform.common.monitor.JstatMonitor;
import com.akaxin.platform.common.monitor.ZalyMonitorController;
import com.zaly.push.constant.AppEnum;
import com.zaly.push.pns.apns.APNsPushManager;
import com.zaly.push.spring.SpringBootStater;

/**
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-08-06 11:43:52
 */
public class BootStrap {
	private static final Logger logger = LoggerFactory.getLogger(BootStrap.class);

	public static void main(String[] args) {

		try {
			initMonitor();
			// load apns for IOS push
			startApnsPush(AppEnum.DUCKCHAT);
//			startApnsPush(AppEnum.AKAXIN);
			SpringBootStater.main(args);
		} catch (Exception e) {
			logger.error("start zaly push eror", e);
			System.exit(-100);
		}
	}

	private static void initMonitor() {
		ZalyMonitorController zmc = new ZalyMonitorController();
		zmc.addMonitor(new JstatMonitor());
		// zmc.addMonitor(new PushMonitor());
		zmc.start();
		logger.info("platform init push monitor");
	}

	private static void startApnsPush(AppEnum app) throws Exception {
		APNsPushManager.getInstance().start(app);
		logger.info("platform start apns manager finish");
	}

}
