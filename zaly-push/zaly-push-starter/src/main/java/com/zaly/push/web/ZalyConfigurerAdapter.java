package com.zaly.push.web;

import javax.servlet.Filter;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootConfiguration
public class ZalyConfigurerAdapter implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
		registry.addResourceHandler("/templates/**").addResourceLocations("classpath:/templates/");
		WebMvcConfigurer.super.addResourceHandlers(registry);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		System.out.println("ZalyConfigurerAdapter -> addInterceptors");
		registry.addInterceptor(new ZalyInterceptor())//
				.addPathPatterns("/**")//
				.excludePathPatterns("/login");
		WebMvcConfigurer.super.addInterceptors(registry);
	}

	/**
	 * rewrite filter
	 * 
	 * @return
	 */
//	@Bean
//	public FilterRegistrationBean<Filter> rebindZalyFilter() {
//		FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<Filter>();
//		ZalyFilter zalyFilter = new ZalyFilter();
//		registration.setFilter(zalyFilter);
//		registration.addUrlPatterns("/*");
//		return registration;
//	}
}
