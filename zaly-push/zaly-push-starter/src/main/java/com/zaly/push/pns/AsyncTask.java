package com.zaly.push.pns;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaly.push.constant.AppEnum;
import com.zaly.push.pns.apns.APNsPackage;
import com.zaly.push.pns.huawei.HmsPackage;
import com.zaly.push.pns.umeng.UmengPackage;
import com.zaly.push.pns.xiaomi.XiaomiPackage;

/**
 * all push task executed by AsyncTask
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-08-08 11:10:23
 */
public class AsyncTask {
	private static final Logger logger = LoggerFactory.getLogger(AsyncTask.class);

	private static ExecutorService executor = new ThreadPoolExecutor(5, 10, 5, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(50), new RejectedExecutionHandler() {

				@Override
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
					logger.error("rejected async push thread activeCount={}, queueSize={}", executor.getActiveCount(),
							executor.getQueue());
				}

			});

	public static boolean execute(final AppEnum app, IPushPackage pack) {

		if (pack == null || pack.getType() == null) {
			return false;
		}

		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
//						logger.info("zaly-push async task app={} pack={}", app, pack.toString());

						switch (pack.getType()) {

						case IOS:
							APNsPackage apk = (APNsPackage) pack;
							PushNotification.pushAPNsNotification(app, apk);
							break;
						case ANDROID:
							// use umeng push
							UmengPackage upk = (UmengPackage) pack;
							PushNotification.pushUMengNotification(app, upk);
							break;
						case XIAOMI:
							XiaomiPackage xpk = (XiaomiPackage) pack;
							PushNotification.pushXiaomiNotification(app, xpk);
							break;
						case HUAWEI:
							HmsPackage hpk = (HmsPackage) pack;
							PushNotification.pushHmsNotification(app, hpk);
							break;
						case UNKNOW_TYPE:
							// error or exception
						default:
							throw new Exception("send push unknow type");
						}
					} catch (Exception e) {
						logger.error("threadpool to async execute push error", e);
					}
				}
			});

			return true;
		} catch (Exception e) {
			logger.error("async execute push error", e);
		}

		return false;
	}

}
