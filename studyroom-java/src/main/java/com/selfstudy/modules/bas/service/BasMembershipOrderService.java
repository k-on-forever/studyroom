package com.selfstudy.modules.bas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.modules.bas.entity.BasMembershipOrderEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface BasMembershipOrderService extends IService<BasMembershipOrderEntity> {

	/** 当前是否在会员有效期内（已激活且 valid_from ≤ now < valid_to） */
	boolean hasActiveMembership(Long userId);

	/** 已激活订单中的最大 valid_to，无则 null */
	Date maxPaidValidTo(Long userId);

	Long createPendingOrder(Long userId, Long cardId, Long couponId);

	boolean mockPayOrder(Long userId, Long orderId);

	/** 首次激活或修改生效日；返回 true 表示本次为修改（非首次） */
	boolean activateOrder(Long userId, Long orderId, Date activateAt);

	int maxActivateChanges();

	/** 可选生效日上限（当天 0 点，含） */
	Date maxAllowedActivateDate();

	/** 是否仍可修改激活日期（未生效且未达修改上限） */
	boolean canModifyActivation(BasMembershipOrderEntity order);

	/** 已支付、未激活的期限卡订单 */
	List<BasMembershipOrderEntity> listPendingActivation(Long userId);

	List<BasMembershipOrderEntity> listMine(Long userId);

	BigDecimal pendingAmount(Long userId, Long orderId);
}
