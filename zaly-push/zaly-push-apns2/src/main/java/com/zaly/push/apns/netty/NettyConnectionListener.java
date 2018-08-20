package com.zaly.push.apns.netty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaly.push.apns.netty.api.INettyConnectionPool;
import com.zaly.push.apns.netty.bean.ConnStateBean;

/**
 * 监控与APNs之间的连接情况,以及进行连接管理工作。
 * 
 * @author Mr.an
 * @since 2017.
 *
 */
public class NettyConnectionListener {
	private static final Logger logger = LoggerFactory.getLogger(NettyConnectionListener.class);

	static {
		Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable thread) {
				String threadName = "netty-client-protect-thread";
				Thread protectThread = new Thread(thread, threadName);
				protectThread.setDaemon(true);
				logger.info("start thread={},Level={}", threadName, "ProtectThread");
				return protectThread;
			}
		}).scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {
				logger.info(protectAndShowConnections(true) + protectAndShowConnections(false));
			}
		}, 5, 15, TimeUnit.SECONDS);

	}

	public static void start() {
		logger.info("start client connection listener!");
	}

	/**
	 * 1.保护Http2长链接 <br>
	 * 2.展示Netty客户端链接情况<br>
	 */
	private static String protectAndShowConnections(boolean isSandbox) {
		StringBuilder checkResult = new StringBuilder("\n[" + (isSandbox ? "SandboxEnv" : "ProductEnv") + "]\n");

		INettyConnectionPool<?> nettyConnectionPool = NettyConnectionPool.getWorkingClientPool(isSandbox);

		if (nettyConnectionPool != null) {

			List<ConnStateBean> stateBean = nettyConnectionPool.showClientDetails();
			for (ConnStateBean bean : stateBean) {
				checkResult.append("-->" + bean.getConnName());
				checkResult.append("-->" + bean.getConnState());
				checkResult.append("-->" + bean.getStreamId());
				checkResult.append("-->" + bean.getResponseSize());
				checkResult.append("\n");
			}

			Map<String, Integer> statesMap = nettyConnectionPool.getClientState();
			if (statesMap != null) {
				checkResult.append("【Connections States】:" + statesMap + "\n");
			} else {
				logger.warn("Can't get 【Client Connections States】....");
			}

		}
		return checkResult.toString();
	}

}
