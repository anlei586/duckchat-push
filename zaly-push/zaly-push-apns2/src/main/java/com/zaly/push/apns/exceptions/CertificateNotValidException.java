package com.zaly.push.apns.exceptions;

/**
 * @author frank@linkedkeeper.com on 2016/12/29.
 */
public class CertificateNotValidException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String reason;

    public CertificateNotValidException(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
