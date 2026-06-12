package com.selfstudy.modules.pay;

import com.selfstudy.modules.applet.dto.save.BasAppointmentSaveDTO;

import java.math.BigDecimal;
import java.util.Map;

public interface WxPayBizService {

	Map<String, Object> clientPayConfig();

	Map<String, Object> prepayAppointment(Long userId, BasAppointmentSaveDTO saveDTO);

	Map<String, Object> prepayMembership(Long userId, Long membershipOrderId);

	/** 演示环境：将待支付单标记为已付 */
	void mockMarkPaid(Long userId, String outTradeNo);

	/**
	 * 预约落库前校验并消费支付单（已支付且金额一致）。
	 */
	void assertAppointmentPaid(Long userId, String outTradeNo, BigDecimal amountYuan);

	void completeMembershipFromPay(String outTradeNo);

	String handlePayNotify(String body, String signature, String nonce, String timestamp, String serial);

	/** 客户端支付成功后同步（会员卡入账；预约由落库接口消费支付单） */
	void afterClientPaid(Long userId, String outTradeNo);
}
