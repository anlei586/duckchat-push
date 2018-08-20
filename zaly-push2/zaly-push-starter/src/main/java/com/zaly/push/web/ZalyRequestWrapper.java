package com.zaly.push.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class ZalyRequestWrapper extends HttpServletRequestWrapper {

	private final byte[] body;

	public ZalyRequestWrapper(HttpServletRequest request) {
		super(request);
		body = ZalyWebUtils.requestToBytes(request);

		System.out.println("~~~~~~~~~~~~~~~~~~~~~ZalyRequestWrapper body=" + body.length);
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream()));
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(body);
		return new ServletInputStream() {

			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public boolean isReady() {
				return false;
			}

			@Override
			public void setReadListener(ReadListener listener) {

			}

			@Override
			public int read() throws IOException {
				return bais.read();
			}
		};
	}
}
