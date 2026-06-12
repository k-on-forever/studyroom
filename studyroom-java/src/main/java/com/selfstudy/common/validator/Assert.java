package com.selfstudy.common.validator;

import com.selfstudy.common.exception.RRException;

public final class Assert {

	private Assert() {
	}

	public static void isNull(Object object, String message) {
		if (object != null) {
			throw new RRException(message);
		}
	}

	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new RRException(message);
		}
	}

	public static void isBlank(String str, String message) {
		if (str == null || str.trim().isEmpty()) {
			throw new RRException(message);
		}
	}
}
