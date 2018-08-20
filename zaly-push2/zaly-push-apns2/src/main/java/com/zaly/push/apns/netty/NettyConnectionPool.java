
package com.zaly.push.apns.netty;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaly.push.apns.constant.ApnsHttp2Config;
import com.zaly.push.apns.netty.api.INettyConnectionPool;
import com.zaly.push.apns.netty.api.INettyHttp2Connection;
import com.zaly.push.apns.netty.bean.ConnStateBean;
import com.zaly.push.apns.notification.IApnsPushNotification;
import com.zaly.push.apns.utils.ClientEntry;
import com.zaly.push.apns.utils.P12Utils;

import io.netty.util.concurrent.Future;

/**
 * netty连接APNs连接池，单例模式 <br>
 * 
 * 总体思想： <br>
 * --------> 构建一个客户端连接池，初始化指定数量N的连接放入连接池ConnPool<br>
 * --------> 使用APNs发送时，先获取一个Client的Http2长连接，然后通过这个长连接发送APNs推送消息<br>
 * --------> 发送完成之后，归还这个Http2长连接给连接池ConnPool
 * 
 * @author Mr.an
 * @since 2017.06.23
 * @param <T>
 */
public class NettyConnectionPool<T extends IApnsPushNotification> implements INettyConnectionPool<T> {
	private static final Logger logger = LoggerFactory.getLogger(NettyConnectionPool.class);

	private static INettyConnectionPool<IApnsPushNotification> instance;
	private static INettyConnectionPool<IApnsPushNotification> sandboxInstance;

	private int CLIENT_INDEX = 0;
	private ApnsHttp2Config config;
	private KeyStore keyStore;
	private Queue<String> clientQueue = null;
	private Map<String, INettyHttp2Connection<T>> clientMap = null;
	private int connPoolSize = 10;

	static {
		NettyConnectionListener.start();
	}

	private NettyConnectionPool(ApnsHttp2Config config) throws KeyStoreException, IOException {
		this.config = config;
		this.keyStore = P12Utils.loadPCKS12KeyStore(config.getKeyStoreStream(), config.getPassword());
		this.connPoolSize = config.getPoolSize();

		clientQueue = new ConcurrentLinkedQueue<String>();
		clientMap = new ConcurrentHashMap<String, INettyHttp2Connection<T>>();

		for (int i = 0; i < this.connPoolSize; i++) {
			try {
				String prefix = config.isSandboxEnvironment() ? "sandbox" : "product";
				String clientName = prefix + "-client-" + i;
				INettyHttp2Connection<T> client = new NettyHttp2Connection<T>(keyStore, this.config.getPassword());
				stablishConnection(client);
				clientQueue.add(clientName);
				clientMap.put(clientName, client);
				logger.info("apns-client {} start to work.OK", clientName);
			} catch (Exception e) {
				logger.error("init connect pool error!", e);
			}
		}
	}

	public static INettyConnectionPool<IApnsPushNotification> createClientPool(ApnsHttp2Config config)
			throws KeyStoreException, IOException {
		if (config.isSandboxEnvironment()) {
			if (sandboxInstance == null) {
				sandboxInstance = new NettyConnectionPool<IApnsPushNotification>(config);
			}
			return sandboxInstance;
		} else {
			if (instance == null) {
				instance = new NettyConnectionPool<IApnsPushNotification>(config);
			}
			return instance;
		}
	}

	public static INettyConnectionPool<IApnsPushNotification> getWorkingClientPool(boolean isSandbox) {
		return isSandbox ? sandboxInstance : instance;
	}

	@Override
	public int getClientQueueSize() {
		return clientQueue.size();
	}

	@Override
	public int getAvailableConnSize() {
		int size = 0;
		for (String connName : clientQueue) {
			if (clientMap.get(connName).isConnected()) {
				size++;
			}
		}
		return size;
	}

	@Override
	public Entry<String, INettyHttp2Connection<T>> acquire() {
		Entry<String, INettyHttp2Connection<T>> clientEntry = null;
		int tryTimes = 0;
		try {
			if (clientQueue != null && clientQueue.size() > 0) {
				for (int i = 0; i < clientQueue.size(); i++) {
					String connName = clientQueue.poll();
					INettyHttp2Connection<T> client = clientMap.get(connName);
					if (client != null && client.isConnected()) {
						clientEntry = new ClientEntry(connName, client);
						return clientEntry;
					} else {
						clientQueue.add(connName);
					}

					if (++tryTimes >= this.connPoolSize) {
						break;
					}
				}
			}
		} catch (Exception e) {
			logger.error("get client connection error! e={}", e);
		}

		return clientEntry;
	}

	@Override
	public void release(Entry<String, INettyHttp2Connection<T>> clientEntry) {
		try {
			clientQueue.add(clientEntry.getKey());
			if (clientEntry.getValue() == null && !clientEntry.getValue().isConnected()) {
				stablishConnection(clientEntry.getValue());
			}
		} catch (Exception e) {
			logger.error("release client connection error!", e);
		}
	}

	@Override
	public void close() {
		for (Entry<String, INettyHttp2Connection<T>> entryClient : clientMap.entrySet()) {
			try {
				if (entryClient.getValue().isConnected()) {
					entryClient.getValue().disconnect();
				}
				logger.info("Close【{}】connection to APNs", entryClient.getKey());
			} catch (Exception e) {
				logger.error("close connections error! e={}", e);
			}
		}
	}

	@Override
	public List<ConnStateBean> showClientDetails() {
		List<ConnStateBean> list = new ArrayList<ConnStateBean>();
		for (Entry<String, INettyHttp2Connection<T>> entryClient : clientMap.entrySet()) {
			ConnStateBean bean = new ConnStateBean();
			try {
				bean.setConnName(entryClient.getKey());
				if (entryClient.getValue().isConnected()) {
					bean.setConnState("true");
				} else {
					reConnection(entryClient.getKey());
					bean.setConnState("doing");
				}
				bean.setStreamId(entryClient.getValue().getStreamId());
				bean.setResponseSize(entryClient.getValue().getResponsePromisesMap().size());
			} catch (Exception e) {
				logger.error("check client state error! key={}", entryClient.getKey());
				bean.setConnState("false");
			}
			list.add(bean);
		}
		return list;
	}

	@Override
	public Map<String, Integer> getClientState() {
		return new HashMap<String, Integer>() {
			{
				int succNum = 0;
				int failNum = 0;
				for (Entry<String, INettyHttp2Connection<T>> entryClient : clientMap.entrySet()) {
					if (entryClient.getValue().isConnected()) {
						succNum++;
					} else {
						failNum++;
					}
				}
				put("Conn-OK", succNum);
				put("Conn-Fail", failNum);
				put("FreeQueue", getClientQueueSize());
				put("Response", getResponseMapSize());
			}
		};
	}

	@Override
	public int getResponseMapSize() {
		int size = 0;
		for (Entry<String, INettyHttp2Connection<T>> conns : clientMap.entrySet()) {
			size += conns.getValue().getResponsePromisesMap().keySet().size();
		}
		return size;
	}

	private void reConnection(String connName) {
		try {
			INettyHttp2Connection<T> client = new NettyHttp2Connection<T>(keyStore, this.config.getPassword());

			stablishConnection(client);
			clientMap.put(connName, client);

			logger.info("Reconnect to APNs finished. result={}", client.isConnected());
		} catch (Exception e) {
			logger.error("reconnect to APNs error! connName={0}" + connName, e);
		}
	}

	private boolean stablishConnection(INettyHttp2Connection<T> client) {
		try {
			final Future<Void> connectFuture = this.config.isSandboxEnvironment() ? client.connectSandBox()
					: client.connectProduction();
			connectFuture.await(1, TimeUnit.SECONDS);
			return true;
		} catch (Exception e) {
			logger.error("stablishConnection error!", e);
		}
		return false;
	}

}
