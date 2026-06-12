package com.selfstudy.config;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "study.wx.pay.mode", havingValue = "wx")
public class WxPayConfiguration {

	@Bean
	public WxPayService wxPayService(StudyWxPayProperties payProps,
			@Value("${applet.wechat.appid:}") String appId) {
		WxPayConfig config = new WxPayConfig();
		config.setAppId(appId);
		config.setMchId(payProps.getMchId());
		config.setApiV3Key(payProps.getApiV3Key());
		config.setPrivateKeyPath(payProps.getPrivateKeyPath());
		config.setCertSerialNo(payProps.getMerchantSerialNumber());
		WxPayServiceImpl service = new WxPayServiceImpl();
		service.setConfig(config);
		return service;
	}
}
