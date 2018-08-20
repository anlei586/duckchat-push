package com.zaly.push.web;

import java.io.ByteArrayOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.zaly.proto.core.Net;

@Component
public class ZalyInterceptor implements HandlerInterceptor {

	/**
	 * dispatch ->preHandler -> controller
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		ByteArrayOutputStream baos = null;
		try {
			byte[] dataBytes = ZalyWebUtils.requestToBytes(request);

			System.out.println("~~~~~~~~~~~~~~~~~~~~~preHandle body=" + dataBytes.length);

			Net.TransportData data = Net.TransportData.parseFrom(dataBytes);

			System.out.println("ZalyInterceptor .preHandle param=" + data.toString());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (baos != null) {
				baos.close();
			}
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
