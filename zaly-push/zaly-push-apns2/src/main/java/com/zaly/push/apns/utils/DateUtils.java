package com.zaly.push.apns.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
	public static String format(long timeMillis, String timePattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(timePattern);
		return sdf.format(new Date(timeMillis));
	}
}
