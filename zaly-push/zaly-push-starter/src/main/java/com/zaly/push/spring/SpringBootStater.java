package com.zaly.push.spring;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <pre>
 * 	在maven modules中，springboot会存在启动main中扫描不到其他modules中的package，两种方法解决：
 * 		其一：@SpringBootApplication(scanBasePackages={"com.akaxin.site.*"})
 * 		其二：SpringApplication.run(Class<?>...clazzs ,args),clazzs 把需要加载的主类添加上
 * </pre>
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-08-06 14:04:34
 */
@SpringBootApplication(scanBasePackages = { "com.zaly.push.*" })
public class SpringBootStater {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(SpringBootStater.class);
		application.setBannerMode(Banner.Mode.OFF);
		application.run(args);

	}
}
