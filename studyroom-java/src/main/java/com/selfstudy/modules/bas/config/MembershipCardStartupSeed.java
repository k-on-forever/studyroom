package com.selfstudy.modules.bas.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.modules.bas.entity.BasMembershipCardEntity;
import com.selfstudy.modules.bas.service.BasMembershipCardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 全新库或空表时自动上架默认会员卡（月 300 / 季 869 / 年 899，与 core.sql 一致）。
 */
@Slf4j
@Component
@Order(100)
public class MembershipCardStartupSeed implements CommandLineRunner {

	private final BasMembershipCardService basMembershipCardService;

	public MembershipCardStartupSeed(BasMembershipCardService basMembershipCardService) {
		this.basMembershipCardService = basMembershipCardService;
	}

	@Override
	public void run(String... args) {
		try {
			if (basMembershipCardService.count(new LambdaQueryWrapper<BasMembershipCardEntity>()
					.eq(BasMembershipCardEntity::getOnShelf, 1)) > 0) {
				return;
			}
			Date now = new Date();
			basMembershipCardService.save(card("MONTH", "月卡", new BigDecimal("300.00"), 30,
					"有效期内不限次数预约免单", 1, 1, now));
			basMembershipCardService.save(card("QUARTER", "季卡", new BigDecimal("869.00"), 90,
					"有效期内不限次数预约免单", 1, 2, now));
			basMembershipCardService.save(card("YEAR", "年卡", new BigDecimal("899.00"), 365,
					"有效期内不限次数预约免单", 1, 3, now));
			log.info("已自动写入默认会员卡（月/季/年）");
		} catch (Exception ex) {
			log.warn("会员卡自动种子跳过: {}", ex.getMessage());
		}
	}

	private static BasMembershipCardEntity card(String kind, String name, BigDecimal price, int days,
			String benefit, int shelf, int sort, Date now) {
		BasMembershipCardEntity e = new BasMembershipCardEntity();
		e.setCardKind(kind);
		e.setBenefitMode(0);
		e.setIncludedHours(null);
		e.setCardName(name);
		e.setPriceYuan(price);
		e.setValidityDays(days);
		e.setBenefitDesc(benefit);
		e.setOnShelf(shelf);
		e.setSortOrder(sort);
		e.setCreateTime(now);
		e.setUpdateTime(now);
		return e;
	}
}
