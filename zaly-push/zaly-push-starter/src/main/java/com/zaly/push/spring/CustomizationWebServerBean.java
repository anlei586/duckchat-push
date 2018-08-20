package com.zaly.push.spring;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomizationWebServerBean implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
	private static Logger logger = LoggerFactory.getLogger(CustomizationWebServerBean.class);

	@Override
	public void customize(ConfigurableServletWebServerFactory server) {

		String host = "0.0.0.0";
		String port = "7050";

		if (StringUtils.isNumeric(port)) {
			server.setPort(Integer.valueOf(port));
		} else {
			server.setPort(7150);
		}

		// set admin address
		if (StringUtils.isNotEmpty(host)) {
			try {
				InetAddress address = InetAddress.getByName(host);
				server.setAddress(address);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		server.setContextPath("/ZalyPush");
	}

}
