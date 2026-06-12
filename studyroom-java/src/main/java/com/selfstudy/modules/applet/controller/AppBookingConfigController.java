package com.selfstudy.modules.applet.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.config.StudyMiniProperties;
import com.selfstudy.modules.pay.WxPayBizService;
import com.selfstudy.modules.sys.service.ReservationRuleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/applet/config")
@Tag(name = "小程序预约配置")
public class AppBookingConfigController {

	private final ReservationProperties reservationProperties;
	private final ReservationRuleConfigService reservationRuleConfig;
	private final StudyMiniProperties studyMiniProperties;
	private final WxPayBizService wxPayBizService;

	public AppBookingConfigController(ReservationProperties reservationProperties,
			ReservationRuleConfigService reservationRuleConfig,
			StudyMiniProperties studyMiniProperties,
			WxPayBizService wxPayBizService) {
		this.reservationProperties = reservationProperties;
		this.reservationRuleConfig = reservationRuleConfig;
		this.studyMiniProperties = studyMiniProperties;
		this.wxPayBizService = wxPayBizService;
	}

	@GetMapping("/booking")
	@Operation(summary = "选座/预约公共配置（时段粒度、营业、支付模式）")
	public R booking() {
		Map<String, Object> data = new HashMap<>();
		data.put("slotMinutes", reservationProperties.getSlotMinutes());
		data.put("dayStart", reservationProperties.getDayStart());
		data.put("dayEnd", reservationProperties.getDayEnd());
		data.put("advanceBookingDays", reservationRuleConfig.getAdvanceBookingDays());
		data.put("perSlotPriceYuan", studyMiniProperties.getPerSlotPriceYuan());
		data.put("pay", wxPayBizService.clientPayConfig());
		return R.ok().put("data", data);
	}
}
