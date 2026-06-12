package com.selfstudy.modules.applet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.applet.annotation.Login;
import com.selfstudy.modules.bas.entity.BasMembershipCardEntity;
import com.selfstudy.modules.bas.entity.BasMembershipOrderEntity;
import com.selfstudy.modules.bas.service.BasMembershipCardService;
import com.selfstudy.modules.bas.service.BasMembershipOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import com.selfstudy.common.exception.RRException;
import com.selfstudy.config.StudyWxPayProperties;
import com.selfstudy.modules.pay.WxPayBizService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applet/membership")
@Tag(name = "小程序会员卡")
public class AppMembershipController {

	private final BasMembershipCardService basMembershipCardService;
	private final BasMembershipOrderService basMembershipOrderService;
	private final StudyWxPayProperties studyWxPayProperties;
	private final WxPayBizService wxPayBizService;

	public AppMembershipController(BasMembershipCardService basMembershipCardService,
			BasMembershipOrderService basMembershipOrderService,
			StudyWxPayProperties studyWxPayProperties,
			WxPayBizService wxPayBizService) {
		this.basMembershipCardService = basMembershipCardService;
		this.basMembershipOrderService = basMembershipOrderService;
		this.studyWxPayProperties = studyWxPayProperties;
		this.wxPayBizService = wxPayBizService;
	}

	@GetMapping("/cards")
	@Operation(summary = "上架会员卡列表（无需登录）")
	public R cards() {
		List<BasMembershipCardEntity> list = basMembershipCardService.list(
				new LambdaQueryWrapper<BasMembershipCardEntity>()
						.eq(BasMembershipCardEntity::getOnShelf, 1)
						.eq(BasMembershipCardEntity::getBenefitMode, 0)
						.orderByAsc(BasMembershipCardEntity::getSortOrder)
						.orderByAsc(BasMembershipCardEntity::getId));
		return R.ok().put("data", list);
	}

	@Login
	@PostMapping("/order")
	@Operation(summary = "创建待支付会员订单")
	public R createOrder(@RequestBody Map<String, Long> body, @RequestAttribute("userId") Long userId) {
		Long cardId = body == null ? null : body.get("cardId");
		if (cardId == null) {
			return R.error("请选择会员卡");
		}
		Long id = basMembershipOrderService.createPendingOrder(userId, cardId, null);
		return R.ok().put("orderId", id);
	}

	@Login
	@PostMapping("/mockPay")
	@Operation(summary = "模拟支付会员订单（仅 study.wx.pay.mode=mock）")
	public R mockPay(@RequestBody Map<String, Long> body, @RequestAttribute("userId") Long userId) {
		if (!studyWxPayProperties.isMockMode()) {
			return R.error("已启用微信支付，请使用 /applet/wxpay/prepay/membership");
		}
		Long orderId = body == null ? null : body.get("orderId");
		if (orderId == null) {
			return R.error("缺少 orderId");
		}
		boolean ok = basMembershipOrderService.mockPayOrder(userId, orderId);
		return ok ? R.ok("支付成功") : R.error("支付失败");
	}

	@Login
	@PostMapping("/activate")
	@Operation(summary = "激活期限会员卡（选择生效日起算有效期）")
	public R activate(@RequestBody Map<String, Object> body, @RequestAttribute("userId") Long userId) {
		Long orderId = body == null ? null : toLong(body.get("orderId"));
		if (orderId == null) {
			return R.error("缺少 orderId");
		}
		String activateDate = body == null ? null : String.valueOf(body.get("activateDate"));
		if (activateDate == null || activateDate.isBlank() || "null".equals(activateDate)) {
			return R.error("请选择激活日期");
		}
		try {
			java.util.Date at = new SimpleDateFormat("yyyy-MM-dd").parse(activateDate.trim().substring(0, 10));
			boolean modified = basMembershipOrderService.activateOrder(userId, orderId, at);
			return R.ok(modified ? "修改成功" : "激活成功");
		} catch (ParseException e) {
			return R.error("日期格式须为 yyyy-MM-dd");
		} catch (RRException e) {
			return R.error(e.getMessage());
		}
	}

	@Login
	@GetMapping("/mine")
	@Operation(summary = "我的会员：当前有效期与订单")
	public R mine(@RequestAttribute("userId") Long userId) {
		java.util.Date end = basMembershipOrderService.maxPaidValidTo(userId);
		boolean active = basMembershipOrderService.hasActiveMembership(userId);
		List<BasMembershipOrderEntity> orders = basMembershipOrderService.listMine(userId);
		List<BasMembershipOrderEntity> pending = basMembershipOrderService.listPendingActivation(userId);
		List<Map<String, Object>> pendingViews = new ArrayList<>();
		for (BasMembershipOrderEntity o : pending) {
			Map<String, Object> row = new HashMap<>();
			row.put("id", o.getId());
			row.put("cardName", o.getCardName());
			row.put("orderNo", o.getOrderNo());
			row.put("payTime", o.getPayTime());
			BasMembershipCardEntity card = basMembershipCardService.getById(o.getCardId());
			row.put("validityDays", card == null ? 30 : card.getValidityDays());
			pendingViews.add(row);
		}
		Map<String, Object> data = new HashMap<>();
		data.put("active", active);
		data.put("validTo", end);
		data.put("pendingActivation", pendingViews);
		data.put("maxActivateChanges", basMembershipOrderService.maxActivateChanges());
		java.util.Date maxAct = basMembershipOrderService.maxAllowedActivateDate();
		data.put("maxActivateDate", maxAct);
		data.put("minActivateDate", startOfToday());
		List<Map<String, Object>> orderViews = new ArrayList<>();
		int maxChg = basMembershipOrderService.maxActivateChanges();
		for (BasMembershipOrderEntity o : orders) {
			Map<String, Object> row = new HashMap<>();
			row.put("id", o.getId());
			row.put("userId", o.getUserId());
			row.put("cardId", o.getCardId());
			row.put("orderNo", o.getOrderNo());
			row.put("cardName", o.getCardName());
			row.put("amountYuan", o.getAmountYuan());
			row.put("payStatus", o.getPayStatus());
			row.put("validFrom", o.getValidFrom());
			row.put("validTo", o.getValidTo());
			row.put("createTime", o.getCreateTime());
			row.put("payTime", o.getPayTime());
			int used = o.getActivateChangeCount() == null ? 0 : o.getActivateChangeCount();
			row.put("activateChangeCount", used);
			row.put("activateChangesLeft", Math.max(0, maxChg - used));
			row.put("canModifyActivate", basMembershipOrderService.canModifyActivation(o));
			orderViews.add(row);
		}
		data.put("orders", orderViews);
		return R.ok().put("data", data);
	}

	private static java.util.Date startOfToday() {
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
		cal.set(java.util.Calendar.MINUTE, 0);
		cal.set(java.util.Calendar.SECOND, 0);
		cal.set(java.util.Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	private static Long toLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number) {
			return ((Number) v).longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
