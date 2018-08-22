package com.zaly.push.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.zaly.proto.core.Net;

//@Component
public class ZalyInterceptor implements HandlerInterceptor {
	private static final Logger logger = LoggerFactory.getLogger(ZalyInterceptor.class);

	/**
	 * dispatch ->preHandler -> controller
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		try {
			byte[] dataBytes = ZalyWebUtils.requestToBytes(request);

			System.out.println("~~~~~~~~~~~~~~~~~~~~~preHandle body=" + dataBytes.length);

			Net.TransportData data = Net.TransportData.parseFrom(dataBytes);

			System.out.println("ZalyInterceptor .preHandle param=" + data.toString());
			return true;
		} catch (Exception e) {
			logger.error("zaly http pre handle", e);
		}
		return false;
	}

	/**
	 * after controller finish
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("ZalyInterceptor .postHandle ");

		// response.
	}

	/**
	 * do clean work
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub

		System.out.println("ZalyInterceptor .afterCompletion ");
	}

}
