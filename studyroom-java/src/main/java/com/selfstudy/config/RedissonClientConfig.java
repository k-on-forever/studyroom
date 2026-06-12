package com.selfstudy.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 与 Spring Data Redis（Lettuce）并行：各用独立连接，共用 spring.data.redis 地址与库号。
 */
@Configuration
public class RedissonClientConfig {

	@Bean(destroyMethod = "shutdown")
	public RedissonClient redissonClient(RedisProperties props) {
		Config config = new Config();
		String schema = props.getSsl().isEnabled() ? "rediss://" : "redis://";
		String address = schema + props.getHost() + ":" + props.getPort();
		var server = config.useSingleServer()
				.setAddress(address)
				.setDatabase(props.getDatabase());
		if (StringUtils.hasText(props.getPassword())) {
			server.setPassword(props.getPassword());
		}
		if (StringUtils.hasText(props.getUsername())) {
			server.setUsername(props.getUsername());
		}
		return Redisson.create(config);
	}
}
