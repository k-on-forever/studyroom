package com.selfstudy.modules.bas.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.entity.BasUserCouponEntity;
import com.selfstudy.modules.bas.service.BasUserCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/bas/coupon")
@RequiredArgsConstructor
@Tag(name = "管理-用户优惠券")
public class SysBasCouponController {

	private final BasUserCouponService basUserCouponService;

	@GetMapping("/list")
	@Operation(summary = "按用户查券")
	public R list(@RequestParam("userId") Long userId) {
		List<BasUserCouponEntity> list = basUserCouponService.lambdaQuery()
				.eq(BasUserCouponEntity::getUserId, userId)
				.orderByDesc(BasUserCouponEntity::getId)
				.list();
		return R.ok().put("data", list);
	}

	@PostMapping("/grant")
	@Operation(summary = "发放代金券（余额型）")
	public R grant(@RequestBody GrantForm form) {
		if (form == null || form.getUserId() == null) {
			return R.error("userId 不能为空");
		}
		if (form.getAmountYuan() == null || form.getAmountYuan().compareTo(BigDecimal.ZERO) <= 0) {
			return R.error("金额须大于 0");
		}
		BasUserCouponEntity e = new BasUserCouponEntity();
		e.setUserId(form.getUserId());
		e.setTitle(StringUtils.hasText(form.getTitle()) ? form.getTitle().trim() : "代金券");
		e.setBalanceYuan(form.getAmountYuan().setScale(2, java.math.RoundingMode.HALF_UP));
		e.setStatus(0);
		e.setExpireTime(form.getExpireTime());
		e.setCreateTime(new Date());
		return basUserCouponService.save(e) ? R.ok() : R.error("保存失败");
	}

	@PostMapping("/void")
	@Operation(summary = "作废优惠券")
	public R voidCoupon(@RequestBody Map<String, Long> body) {
		Long id = body == null ? null : body.get("id");
		if (id == null) {
			return R.error("id 不能为空");
		}
		BasUserCouponEntity e = basUserCouponService.getById(id);
		if (e == null) {
			return R.error("不存在");
		}
		e.setStatus(2);
		return basUserCouponService.updateById(e) ? R.ok() : R.error("更新失败");
	}

	@Data
	public static class GrantForm {
		private Long userId;
		private BigDecimal amountYuan;
		private String title;
		private Date expireTime;
	}
}
