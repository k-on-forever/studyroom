package com.selfstudy.modules.bas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.common.exception.RRException;
import com.selfstudy.config.StudyMiniProperties;
import com.selfstudy.modules.bas.dao.BasMembershipOrderDao;
import com.selfstudy.modules.bas.entity.BasMembershipCardEntity;
import com.selfstudy.modules.bas.entity.BasMembershipOrderEntity;
import com.selfstudy.modules.bas.service.BasMembershipCardService;
import com.selfstudy.modules.bas.service.BasMembershipOrderService;
import com.selfstudy.modules.bas.service.BasUserCouponService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class BasMembershipOrderServiceImpl extends ServiceImpl<BasMembershipOrderDao, BasMembershipOrderEntity>
		implements BasMembershipOrderService {

	private static final int BENEFIT_PERIOD = 0;
	private static final int MAX_ACTIVATE_CHANGES = 3;

	private final BasMembershipCardService basMembershipCardService;
	private final BasUserCouponService basUserCouponService;
	private final StudyMiniProperties studyMiniProperties;

	public BasMembershipOrderServiceImpl(BasMembershipCardService basMembershipCardService,
			BasUserCouponService basUserCouponService,
			StudyMiniProperties studyMiniProperties) {
		this.basMembershipCardService = basMembershipCardService;
		this.basUserCouponService = basUserCouponService;
		this.studyMiniProperties = studyMiniProperties;
	}

	@Override
	public boolean hasActiveMembership(Long userId) {
		Date now = new Date();
		for (BasMembershipOrderEntity o : listActivatedPeriodOrders(userId)) {
			Date from = o.getValidFrom();
			Date to = o.getValidTo();
			if (from != null && !from.after(now) && to != null && to.after(now)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Date maxPaidValidTo(Long userId) {
		Date now = new Date();
		Date max = null;
		for (BasMembershipOrderEntity o : listActivatedPeriodOrders(userId)) {
			Date vt = o.getValidTo();
			if (vt != null && vt.after(now) && (max == null || vt.after(max))) {
				max = vt;
			}
		}
		return max;
	}

	private List<BasMembershipOrderEntity> listActivatedPeriodOrders(Long userId) {
		List<BasMembershipOrderEntity> list = list(new LambdaQueryWrapper<BasMembershipOrderEntity>()
				.eq(BasMembershipOrderEntity::getUserId, userId)
				.eq(BasMembershipOrderEntity::getPayStatus, 1)
				.isNotNull(BasMembershipOrderEntity::getValidFrom)
				.isNotNull(BasMembershipOrderEntity::getValidTo)
				.orderByDesc(BasMembershipOrderEntity::getValidTo));
		return list;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long createPendingOrder(Long userId, Long cardId, Long couponId) {
		BasMembershipCardEntity card = basMembershipCardService.getById(cardId);
		if (card == null || card.getOnShelf() == null || card.getOnShelf() != 1) {
			throw new RRException("会员卡不存在或已下架");
		}
		if (couponId != null) {
			throw new RRException("暂不支持优惠券");
		}
		BigDecimal price = card.getPriceYuan() == null ? BigDecimal.ZERO : card.getPriceYuan();
		BigDecimal discount = BigDecimal.ZERO;
		BigDecimal payable = price.subtract(discount).setScale(2, RoundingMode.HALF_UP);
		if (payable.signum() < 0) {
			payable = BigDecimal.ZERO;
		}
		BasMembershipOrderEntity o = new BasMembershipOrderEntity();
		o.setUserId(userId);
		o.setCardId(cardId);
		o.setCouponId(couponId);
		o.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
		o.setCardName(card.getCardName());
		o.setAmountYuan(payable);
		o.setDiscountYuan(discount);
		o.setPayStatus(0);
		o.setCreateTime(new Date());
		save(o);
		return o.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean mockPayOrder(Long userId, Long orderId) {
		BasMembershipOrderEntity o = getById(orderId);
		if (o == null || !userId.equals(o.getUserId())) {
			return false;
		}
		if (o.getPayStatus() != null && o.getPayStatus() == 1) {
			return true;
		}
		BasMembershipCardEntity card = basMembershipCardService.getById(o.getCardId());
		if (card == null) {
			return false;
		}
		if (o.getCouponId() != null && o.getDiscountYuan() != null && o.getDiscountYuan().signum() > 0) {
			basUserCouponService.consumeBalance(userId, o.getCouponId(), o.getDiscountYuan());
		}
		Date now = new Date();
		// 期限卡：支付后不自动开通，由用户在小程序选择激活日起算
		o.setValidFrom(null);
		o.setValidTo(null);
		o.setPayStatus(1);
		o.setPayTime(now);
		return updateById(o);
	}

	@Override
	public int maxActivateChanges() {
		return MAX_ACTIVATE_CHANGES;
	}

	@Override
	public Date maxAllowedActivateDate() {
		int years = studyMiniProperties.getMembershipMaxActivateYears();
		if (years < 1) {
			years = 1;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(startOfDay(new Date()));
		cal.add(Calendar.YEAR, years);
		return cal.getTime();
	}

	@Override
	public boolean canModifyActivation(BasMembershipOrderEntity o) {
		if (o == null || o.getPayStatus() == null || o.getPayStatus() != 1 || o.getValidFrom() == null) {
			return false;
		}
		BasMembershipCardEntity card = basMembershipCardService.getById(o.getCardId());
		if (card == null) {
			return false;
		}
		Date now = new Date();
		if (!o.getValidFrom().after(now)) {
			return false;
		}
		int used = o.getActivateChangeCount() == null ? 0 : o.getActivateChangeCount();
		return used < MAX_ACTIVATE_CHANGES;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean activateOrder(Long userId, Long orderId, Date activateAt) {
		if (activateAt == null) {
			throw new RRException("请选择激活日期");
		}
		BasMembershipOrderEntity o = getById(orderId);
		if (o == null || !userId.equals(o.getUserId())) {
			throw new RRException("订单不存在");
		}
		if (o.getPayStatus() == null || o.getPayStatus() != 1) {
			throw new RRException("请先完成支付");
		}
		BasMembershipCardEntity card = basMembershipCardService.getById(o.getCardId());
		if (card == null) {
			throw new RRException("会员卡不存在");
		}
		Date startOfToday = startOfDay(new Date());
		Date activateDay = startOfDay(activateAt);
		if (activateDay.before(startOfToday)) {
			throw new RRException("激活日期不能早于今天");
		}
		Date maxActivateDay = startOfDay(maxAllowedActivateDate());
		if (activateDay.after(maxActivateDay)) {
			int years = Math.max(1, studyMiniProperties.getMembershipMaxActivateYears());
			throw new RRException("生效日期最晚可选为 " + formatYmd(maxActivateDay) + "（自今天起 " + years + " 年内）");
		}
		boolean isModify = o.getValidFrom() != null;
		if (isModify) {
			if (!canModifyActivation(o)) {
				int used = o.getActivateChangeCount() == null ? 0 : o.getActivateChangeCount();
				if (used >= MAX_ACTIVATE_CHANGES) {
					throw new RRException("该卡激活日期最多可修改" + MAX_ACTIVATE_CHANGES + "次，已达上限");
				}
				throw new RRException("会员已生效或已过期，无法修改激活日期");
			}
		}
		applyPeriodActivation(o, card, activateDay);
		if (isModify) {
			int used = o.getActivateChangeCount() == null ? 0 : o.getActivateChangeCount();
			o.setActivateChangeCount(used + 1);
		} else if (o.getActivateChangeCount() == null) {
			o.setActivateChangeCount(0);
		}
		updateById(o);
		return isModify;
	}

	private void applyPeriodActivation(BasMembershipOrderEntity o, BasMembershipCardEntity card, Date activateDay) {
		int days = card.getValidityDays() == null ? 30 : card.getValidityDays();
		if (days <= 0) {
			throw new RRException("会员卡有效期配置错误");
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(activateDay);
		cal.add(Calendar.DATE, days);
		o.setValidFrom(activateDay);
		o.setValidTo(cal.getTime());
	}

	@Override
	public List<BasMembershipOrderEntity> listPendingActivation(Long userId) {
		List<BasMembershipOrderEntity> list = list(new LambdaQueryWrapper<BasMembershipOrderEntity>()
				.eq(BasMembershipOrderEntity::getUserId, userId)
				.eq(BasMembershipOrderEntity::getPayStatus, 1)
				.isNull(BasMembershipOrderEntity::getValidFrom)
				.orderByDesc(BasMembershipOrderEntity::getPayTime));
		return list;
	}

	private static Date startOfDay(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	private static String formatYmd(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		int y = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		return String.format("%04d-%02d-%02d", y, m, day);
	}

	@Override
	public List<BasMembershipOrderEntity> listMine(Long userId) {
		return list(new LambdaQueryWrapper<BasMembershipOrderEntity>()
				.eq(BasMembershipOrderEntity::getUserId, userId)
				.orderByDesc(BasMembershipOrderEntity::getCreateTime));
	}

	@Override
	public BigDecimal pendingAmount(Long userId, Long orderId) {
		BasMembershipOrderEntity o = getById(orderId);
		if (o == null || !userId.equals(o.getUserId()) || o.getPayStatus() == null || o.getPayStatus() != 0) {
			return null;
		}
		return o.getAmountYuan();
	}
}
