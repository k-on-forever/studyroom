package com.selfstudy.common.exception;

/**
 * 业务异常（人人风格）。
 */
public class RRException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private int code = 500;

	public RRException(String msg) {
		super(msg);
	}

	public RRException(String msg, int code) {
		super(msg);
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
