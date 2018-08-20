package com.zaly.push.apns.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理APNs Token
 * 
 * @author Mr.an
 * @since 2017.06.13
 *
 */
public class APNsTokenPattern {
	private static String regEx = "[`~ !@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";

	public static String formatToken(String token) {
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(token);
		return m.replaceAll("").trim();
	}

}
