package com.selfstudy.modules.bas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.entity.BasMembershipCardEntity;
import com.selfstudy.modules.bas.service.BasMembershipCardService;
import com.selfstudy.common.annotation.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/membership-card")
@Tag(name = "管理-会员卡")
public class SysBasMembershipCardController {

	private final BasMembershipCardService basMembershipCardService;

	@GetMapping("/list")
	@Operation(summary = "列表（含下架）")
	public R list() {
		List<BasMembershipCardEntity> list = basMembershipCardService.list(
				new LambdaQueryWrapper<BasMembershipCardEntity>()
						.orderByAsc(BasMembershipCardEntity::getSortOrder)
						.orderByAsc(BasMembershipCardEntity::getId));
		return R.ok().put("data", list);
	}

	@GetMapping("/info/{id}")
	@Operation(summary = "详情")
	public R info(@PathVariable("id") Long id) {
		BasMembershipCardEntity e = basMembershipCardService.getById(id);
		if (e == null) {
			return R.error("记录不存在");
		}
		return R.ok().put("data", e);
	}

	@PostMapping("/save")
	@SysLog("新增会员卡")
	@Operation(summary = "新增")
	public R save(@RequestBody CardForm form) {
		R bad = validateForm(form);
		if (bad != null) {
			return bad;
		}
		BasMembershipCardEntity e = new BasMembershipCardEntity();
		fillEntity(e, form);
		e.setCreateTime(new Date());
		return basMembershipCardService.save(e) ? R.ok() : R.error("保存失败");
	}

	@PostMapping("/update")
	@SysLog("修改会员卡")
	@Operation(summary = "修改")
	public R update(@RequestBody CardForm form) {
		if (form.getId() == null) {
			return R.error("id 不能为空");
		}
		R bad = validateForm(form);
		if (bad != null) {
			return bad;
		}
		BasMembershipCardEntity e = basMembershipCardService.getById(form.getId());
		if (e == null) {
			return R.error("记录不存在");
		}
		fillEntity(e, form);
		e.setUpdateTime(new Date());
		return basMembershipCardService.updateById(e) ? R.ok() : R.error("更新失败");
	}

	@PostMapping("/shelf")
	@SysLog("上架/下架会员卡")
	@Operation(summary = "上架/下架")
	public R shelf(@RequestBody Map<String, Object> body) {
		if (body == null || body.get("id") == null) {
			return R.error("id 不能为空");
		}
		long id = Long.parseLong(String.valueOf(body.get("id")));
		int on = body.get("onShelf") != null && Integer.parseInt(String.valueOf(body.get("onShelf"))) == 1 ? 1 : 0;
		BasMembershipCardEntity e = basMembershipCardService.getById(id);
		if (e == null) {
			return R.error("记录不存在");
		}
		e.setOnShelf(on);
		e.setUpdateTime(new Date());
		return basMembershipCardService.updateById(e) ? R.ok() : R.error("更新失败");
	}

	@PostMapping("/delete")
	@SysLog("删除会员卡")
	@Operation(summary = "删除")
	public R delete(@RequestBody Map<String, Long> body) {
		Long id = body == null ? null : body.get("id");
		if (id == null) {
			return R.error("id 不能为空");
		}
		return basMembershipCardService.removeById(id) ? R.ok() : R.error("删除失败");
	}

	private static void fillEntity(BasMembershipCardEntity e, CardForm form) {
		e.setCardKind(form.getCardKind().trim().toUpperCase());
		int bm = form.getBenefitMode() == null ? 0 : form.getBenefitMode();
		e.setBenefitMode(bm);
		e.setCardName(form.getCardName().trim());
		e.setPriceYuan(form.getPriceYuan());
		e.setValidityDays(form.getValidityDays());
		e.setIncludedHours(form.getIncludedHours());
		e.setBenefitDesc(StringUtils.hasText(form.getBenefitDesc()) ? form.getBenefitDesc().trim() : null);
		e.setOnShelf(form.getOnShelf() != null && form.getOnShelf() == 1 ? 1 : 0);
		e.setSortOrder(form.getSortOrder() != null ? form.getSortOrder() : 0);
	}

	private R validateForm(CardForm form) {
		if (form == null) {
			return R.error("参数不能为空");
		}
		if (!StringUtils.hasText(form.getCardKind())) {
			return R.error("卡类型不能为空");
		}
		String k = form.getCardKind().trim().toUpperCase();
		if (!k.matches("^(MONTH|QUARTER|YEAR|OTHER|HOUR_[A-Z0-9_]+|[A-Z][A-Z0-9_]{0,28})$")) {
			return R.error("卡类型编码不合法（如 MONTH、HOUR_4）");
		}
		if (!StringUtils.hasText(form.getCardName())) {
			return R.error("名称不能为空");
		}
		if (form.getPriceYuan() == null || form.getPriceYuan().compareTo(BigDecimal.ZERO) < 0) {
			return R.error("价格不能为负");
		}
		if (form.getValidityDays() == null || form.getValidityDays() < 1) {
			return R.error("有效天数须 ≥ 1");
		}
		int bm = form.getBenefitMode() == null ? 0 : form.getBenefitMode();
		if (bm == 1) {
			if (form.getIncludedHours() == null || form.getIncludedHours().compareTo(BigDecimal.ZERO) <= 0) {
				return R.error("学时包须填写「包含学时」且大于 0");
			}
		}
		return null;
	}

	@Data
	public static class CardForm {
		private Long id;
		private String cardKind;
		/** 0 期限畅约 1 学时包 */
		private Integer benefitMode;
		private String cardName;
		private BigDecimal priceYuan;
		private Integer validityDays;
		private BigDecimal includedHours;
		private String benefitDesc;
		private Integer onShelf;
		private Integer sortOrder;
	}
}
