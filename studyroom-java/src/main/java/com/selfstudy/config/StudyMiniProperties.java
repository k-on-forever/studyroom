package com.selfstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 小程序等非会员相关配置（计价、注册短信等）。
 */
@Component
@ConfigurationProperties(prefix = "study.mini")
public class StudyMiniProperties {

	/** 非会员现金单价（元/小时），与 {@code study.reservation.slot-minutes} 无关；应付 = 单价×实际时长(小时) */
	private double perSlotPriceYuan = 6.0;
	/** 期限卡可选生效日：自今天起最多向后选多少年（含当天算第 0 年） */
	private int membershipMaxActivateYears = 2;

	public double getPerSlotPriceYuan() {
		return perSlotPriceYuan;
	}

	public void setPerSlotPriceYuan(double perSlotPriceYuan) {
		this.perSlotPriceYuan = perSlotPriceYuan;
	}

	public int getMembershipMaxActivateYears() {
		return membershipMaxActivateYears;
	}

	public void setMembershipMaxActivateYears(int membershipMaxActivateYears) {
		this.membershipMaxActivateYears = membershipMaxActivateYears;
	}
}
