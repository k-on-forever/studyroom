package com.selfstudy.modules.applet.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppointmentBookResult {
	private boolean ok;
	private boolean needSimulatePay;
	private BigDecimal amountYuan;
	private int slotCount;
	private String message;

	public static AppointmentBookResult success() {
		AppointmentBookResult r = new AppointmentBookResult();
		r.setOk(true);
		return r;
	}

	public static AppointmentBookResult needPay(BigDecimal amountYuan, int slotCount) {
		AppointmentBookResult r = new AppointmentBookResult();
		r.setNeedSimulatePay(true);
		r.setAmountYuan(amountYuan);
		r.setSlotCount(slotCount);
		r.setMessage("请先完成模拟支付");
		return r;
	}

	public static AppointmentBookResult fail(String message) {
		AppointmentBookResult r = new AppointmentBookResult();
		r.setMessage(message);
		return r;
	}
}
