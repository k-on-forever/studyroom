package com.selfstudy.modules.applet.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WxMiniAppConfiguration {

	@Bean
	public WxMaDefaultConfigImpl wxMaConfig(
			@Value("${applet.wechat.appid:placeholder}") String appid,
			@Value("${applet.wechat.secret:placeholder}") String secret) {
		WxMaDefaultConfigImpl c = new WxMaDefaultConfigImpl();
		c.setAppid(appid);
		c.setSecret(secret);
		return c;
	}

	@Bean
	public WxMaService wxMaService(WxMaDefaultConfigImpl wxMaConfig) {
		WxMaServiceImpl s = new WxMaServiceImpl();
		s.setWxMaConfig(wxMaConfig);
		return s;
	}
}
