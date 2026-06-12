package com.selfstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信小程序支付：mode=mock 时允许演示标记落单；mode=wx 时须配置商户号与证书。
 */
@Component
@ConfigurationProperties(prefix = "study.wx.pay")
public class StudyWxPayProperties {

	/** mock：演示支付标记；wx：真实微信支付 */
	private String mode = "mock";

	private String mchId = "";

	private String apiV3Key = "";

	private String privateKeyPath = "";

	private String merchantSerialNumber = "";

	/** 公网 HTTPS，如 https://域名/self-study/applet/wxpay/notify */
	private String notifyUrl = "";

	public boolean isMockMode() {
		return !"wx".equalsIgnoreCase(mode == null ? "" : mode.trim());
	}

	public boolean isWxConfigured() {
		return StringUtils.hasText(mchId)
				&& StringUtils.hasText(apiV3Key)
				&& StringUtils.hasText(privateKeyPath)
				&& StringUtils.hasText(merchantSerialNumber)
				&& StringUtils.hasText(notifyUrl);
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getMchId() {
		return mchId;
	}

	public void setMchId(String mchId) {
		this.mchId = mchId;
	}

	public String getApiV3Key() {
		return apiV3Key;
	}

	public void setApiV3Key(String apiV3Key) {
		this.apiV3Key = apiV3Key;
	}

	public String getPrivateKeyPath() {
		return privateKeyPath;
	}

	public void setPrivateKeyPath(String privateKeyPath) {
		this.privateKeyPath = privateKeyPath;
	}

	public String getMerchantSerialNumber() {
		return merchantSerialNumber;
	}

	public void setMerchantSerialNumber(String merchantSerialNumber) {
		this.merchantSerialNumber = merchantSerialNumber;
	}

	public String getNotifyUrl() {
		return notifyUrl;
	}

	public void setNotifyUrl(String notifyUrl) {
		this.notifyUrl = notifyUrl;
	}
}
