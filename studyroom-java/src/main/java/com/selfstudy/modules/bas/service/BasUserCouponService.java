package com.selfstudy.modules.bas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.modules.bas.entity.BasUserCouponEntity;

import java.math.BigDecimal;
import java.util.List;

public interface BasUserCouponService extends IService<BasUserCouponEntity> {

	List<BasUserCouponEntity> listUsable(Long userId);

	/** 可抵用金额（不超过订单原价），不落库 */
	BigDecimal computeDiscount(Long userId, Long couponId, BigDecimal orderAmountYuan);

	/** 支付时核销，与订单支付同事务 */
	void consumeBalance(Long userId, Long couponId, BigDecimal discountYuan);
}
