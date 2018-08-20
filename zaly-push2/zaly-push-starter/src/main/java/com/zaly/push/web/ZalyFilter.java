package com.zaly.push.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class ZalyFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		ServletRequest requestWrapper = null;

		if (request instanceof HttpServletRequest) {
			requestWrapper = new ZalyRequestWrapper((HttpServletRequest) request);
		}

		if (null == requestWrapper) {
			chain.doFilter(request, response);
		} else {
			chain.doFilter(requestWrapper, response);
		}

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
