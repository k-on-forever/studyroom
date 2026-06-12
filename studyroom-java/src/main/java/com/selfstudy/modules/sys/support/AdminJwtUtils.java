package com.selfstudy.modules.sys.support;

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
 * 管理后台 JWT（与小程序 {@code JwtUtils} 密钥隔离，避免 token 混用）。
 */
@ConfigurationProperties(prefix = "study.admin.jwt")
@Component
public class AdminJwtUtils {
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

	public String generateToken(long adminId) {
		Date nowDate = new Date();
		Date expireDate = new Date(nowDate.getTime() + expire * 1000);
		return Jwts.builder()
				.subject(String.valueOf(adminId))
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
			logger.debug("admin jwt validate error", e);
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
