package com.selfstudy.modules.applet.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * jwt工具类（JJWT 0.12 + Spring Boot 3 / Jakarta）
 */
@ConfigurationProperties(prefix = "applet.jwt")
@Component
public class JwtUtils {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private String secret;
	private long expire;
	private String header;

	private SecretKey signKey;

	private SecretKey resolveSignKey() {
		if (signKey != null) {
			return signKey;
		}
		try {
			byte[] raw = secret != null ? secret.getBytes(StandardCharsets.UTF_8) : new byte[0];
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw);
			signKey = Keys.hmacShaKeyFor(hash);
			return signKey;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public String generateToken(long userId) {
		Date nowDate = new Date();
		Date expireDate = new Date(nowDate.getTime() + expire * 1000);
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.issuedAt(nowDate)
				.expiration(expireDate)
				.signWith(resolveSignKey())
				.compact();
	}

	public Claims getClaimByToken(String token) {
		try {
			return Jwts.parser()
					.verifyWith(resolveSignKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (Exception e) {
			logger.debug("validate is token error ", e);
			return null;
		}
	}

	public boolean isTokenExpired(Date expiration) {
		return expiration.before(new Date());
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
		this.signKey = null;
	}

	public long getExpire() {
		return expire;
	}

	public void setExpire(long expire) {
		this.expire = expire;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}
}
