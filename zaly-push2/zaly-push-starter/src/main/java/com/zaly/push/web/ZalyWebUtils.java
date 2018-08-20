package com.zaly.push.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

public class ZalyWebUtils {

	public static byte[] requestToBytes(HttpServletRequest request) {
		ByteArrayOutputStream baos = null;
		ServletInputStream sis = null;

		try {
			sis = request.getInputStream();
			baos = new ByteArrayOutputStream();
			byte buff[] = new byte[256];
			int len = 0;
			while ((len = sis.read(buff)) > 0) {
				baos.write(buff, 0, len);
			}

			return baos.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (sis != null) {
				try {
					sis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;
	}

}
