package com.selfstudy.modules.sys.oauth2;

import java.util.UUID;

public final class TokenGenerator {

	private TokenGenerator() {
	}

	public static String generateValue() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
