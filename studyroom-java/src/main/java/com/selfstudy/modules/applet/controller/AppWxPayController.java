package com.selfstudy.modules.applet.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.applet.annotation.Login;
import com.selfstudy.modules.applet.dto.save.BasAppointmentSaveDTO;
import com.selfstudy.modules.pay.WxPayBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/applet/wxpay")
@Tag(name = "小程序微信支付")
public class AppWxPayController {

	private final WxPayBizService wxPayBizService;

	public AppWxPayController(WxPayBizService wxPayBizService) {
		this.wxPayBizService = wxPayBizService;
	}

	@GetMapping("/config")
	@Operation(summary = "支付模式（mock / wx）")
	public R config() {
		return R.ok().put("data", wxPayBizService.clientPayConfig());
	}

	@Login
	@PostMapping("/prepay/appointment")
	@Operation(summary = "预约下单预支付")
	public R prepayAppointment(@RequestBody BasAppointmentSaveDTO saveDTO,
			@RequestAttribute("userId") Long userId) {
		return R.ok().put("data", wxPayBizService.prepayAppointment(userId, saveDTO));
	}

	@Login
	@PostMapping("/prepay/membership")
	@Operation(summary = "会员卡预支付")
	public R prepayMembership(@RequestBody Map<String, Long> body, @RequestAttribute("userId") Long userId) {
		Long orderId = body == null ? null : body.get("orderId");
		if (orderId == null) {
			return R.error("缺少 orderId");
		}
		return R.ok().put("data", wxPayBizService.prepayMembership(userId, orderId));
	}

	@Login
	@PostMapping("/mock/confirm")
	@Operation(summary = "演示环境：确认模拟支付成功")
	public R mockConfirm(@RequestBody Map<String, String> body, @RequestAttribute("userId") Long userId) {
		String outTradeNo = body == null ? null : body.get("outTradeNo");
		if (outTradeNo == null || outTradeNo.isBlank()) {
			return R.error("缺少 outTradeNo");
		}
		wxPayBizService.mockMarkPaid(userId, outTradeNo.trim());
		return R.ok();
	}

	@Login
	@PostMapping("/after-pay")
	@Operation(summary = "客户端支付成功回调（会员卡入账）")
	public R afterPay(@RequestBody Map<String, String> body, @RequestAttribute("userId") Long userId) {
		String outTradeNo = body == null ? null : body.get("outTradeNo");
		if (outTradeNo == null || outTradeNo.isBlank()) {
			return R.error("缺少 outTradeNo");
		}
		wxPayBizService.afterClientPaid(userId, outTradeNo.trim());
		return R.ok();
	}

	@PostMapping("/notify")
	@Operation(summary = "微信支付结果通知（微信服务器回调，勿加登录）")
	public String notify(HttpServletRequest request, @RequestBody String body) {
		String signature = request.getHeader("Wechatpay-Signature");
		String nonce = request.getHeader("Wechatpay-Nonce");
		String timestamp = request.getHeader("Wechatpay-Timestamp");
		String serial = request.getHeader("Wechatpay-Serial");
		return wxPayBizService.handlePayNotify(body, signature, nonce, timestamp, serial);
	}
}
