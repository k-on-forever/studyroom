package com.selfstudy.modules.bas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.common.exception.RRException;
import com.selfstudy.modules.bas.dao.BasUserCouponDao;
import com.selfstudy.modules.bas.entity.BasUserCouponEntity;
import com.selfstudy.modules.bas.service.BasUserCouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class BasUserCouponServiceImpl extends ServiceImpl<BasUserCouponDao, BasUserCouponEntity>
		implements BasUserCouponService {

	@Override
	public List<BasUserCouponEntity> listUsable(Long userId) {
		Date now = new Date();
		return list(new LambdaQueryWrapper<BasUserCouponEntity>()
				.eq(BasUserCouponEntity::getUserId, userId)
				.eq(BasUserCouponEntity::getStatus, 0)
				.gt(BasUserCouponEntity::getBalanceYuan, BigDecimal.ZERO)
				.and(w -> w.isNull(BasUserCouponEntity::getExpireTime).or().gt(BasUserCouponEntity::getExpireTime, now))
				.orderByDesc(BasUserCouponEntity::getId));
	}

	@Override
	public BigDecimal computeDiscount(Long userId, Long couponId, BigDecimal orderAmountYuan) {
		if (couponId == null || orderAmountYuan == null || orderAmountYuan.signum() <= 0) {
			return BigDecimal.ZERO;
		}
		BasUserCouponEntity c = getById(couponId);
		if (c == null || !userId.equals(c.getUserId()) || c.getStatus() == null || c.getStatus() != 0) {
			throw new RRException("优惠券不可用");
		}
		if (c.getExpireTime() != null && !c.getExpireTime().after(new Date())) {
			throw new RRException("优惠券已过期");
		}
		BigDecimal bal = c.getBalanceYuan() == null ? BigDecimal.ZERO : c.getBalanceYuan();
		if (bal.signum() <= 0) {
			throw new RRException("优惠券余额不足");
		}
		BigDecimal d = bal.min(orderAmountYuan);
		return d.setScale(2, java.math.RoundingMode.HALF_UP);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void consumeBalance(Long userId, Long couponId, BigDecimal discountYuan) {
		if (couponId == null || discountYuan == null || discountYuan.signum() <= 0) {
			return;
		}
		BasUserCouponEntity c = getById(couponId);
		if (c == null || !userId.equals(c.getUserId()) || c.getStatus() == null || c.getStatus() != 0) {
			throw new RRException("优惠券核销失败（不可用）");
		}
		if (c.getExpireTime() != null && !c.getExpireTime().after(new Date())) {
			throw new RRException("优惠券已过期");
		}
		BigDecimal bal = c.getBalanceYuan() == null ? BigDecimal.ZERO : c.getBalanceYuan();
		if (bal.compareTo(discountYuan) < 0) {
			throw new RRException("优惠券余额不足");
		}
		BigDecimal nb = bal.subtract(discountYuan).setScale(2, RoundingMode.HALF_UP);
		c.setBalanceYuan(nb);
		c.setStatus(nb.signum() <= 0 ? 1 : 0);
		if (!updateById(c)) {
			throw new RRException("优惠券核销失败");
		}
	}
}
