package com.selfstudy.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.modules.sys.dao.SysReservationRuleDao;
import com.selfstudy.modules.sys.entity.SysReservationRuleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Date;

/**
 * 预约规则（库表 sys_reservation_rule，id 固定 1）。<br>
 * 单次连续预约最长时长<strong>不单独设上限</strong>，与 {@code study.reservation.day-start/day-end} 营业总跨度一致；
 * 同一天内时段由 {@code TimeSlotCodec} 与营业窗校验；提前预约天数仍由本表维护。
 */
@Service
public class ReservationRuleConfigService extends ServiceImpl<SysReservationRuleDao, SysReservationRuleEntity> {

	public static final int DEFAULT_ID = 1;

	@Autowired
	private ReservationProperties reservationProperties;

	/** 可预约：相对今天最远间隔天数（含今天为第 0 天则 today+0 为今天） */
	public int getAdvanceBookingDays() {
		SysReservationRuleEntity e = ensure();
		return e.getAdvanceBookingDays() != null ? e.getAdvanceBookingDays() : 7;
	}

	/**
	 * 全局营业窗总分钟数（如 08:00–22:30 → 870），即单次连续预约允许的上限（与「仅同一天」规则叠加）。
	 */
	public int getOperatingSpanMinutes() {
		LocalTime a = LocalTime.parse(reservationProperties.getDayStart().trim());
		LocalTime b = LocalTime.parse(reservationProperties.getDayEnd().trim());
		return (int) Duration.between(a, b).toMinutes();
	}

	/**
	 * 单次连续预约最大总时长（分钟）：恒为当日营业窗跨度，不再读库字段（库列仅作展示同步）。
	 */
	public int getMaxDurationMinutes() {
		return getOperatingSpanMinutes();
	}

	/** 距预约开始时间不足此分钟数则不可自行取消 */
	public int getCancelAdvanceMinutes() {
		SysReservationRuleEntity e = ensure();
		return e.getCancelAdvanceMinutes() != null ? e.getCancelAdvanceMinutes() : 30;
	}

	public SysReservationRuleEntity getForAdmin() {
		SysReservationRuleEntity e = ensure();
		// 列表/表单展示与真实校验一致，避免库里残留 240 误导
		e.setMaxDurationMinutes(getOperatingSpanMinutes());
		return e;
	}

	@Transactional(rollbackFor = Exception.class)
	public void saveRule(SysReservationRuleEntity in) {
		if (in.getAdvanceBookingDays() == null || in.getAdvanceBookingDays() < 0) {
			throw new IllegalArgumentException("提前预约天数不合法");
		}
		if (in.getCancelAdvanceMinutes() == null || in.getCancelAdvanceMinutes() < 0) {
			throw new IllegalArgumentException("取消提前时间(分钟)不合法");
		}
		int span = getOperatingSpanMinutes();
		SysReservationRuleEntity e = new SysReservationRuleEntity();
		e.setId(DEFAULT_ID);
		e.setAdvanceBookingDays(in.getAdvanceBookingDays());
		e.setMaxDurationMinutes(span);
		e.setCancelAdvanceMinutes(in.getCancelAdvanceMinutes());
		e.setUpdateTime(new Date());
		this.saveOrUpdate(e);
	}

	private SysReservationRuleEntity ensure() {
		SysReservationRuleEntity e = getById(DEFAULT_ID);
		if (e == null) {
			e = new SysReservationRuleEntity();
			e.setId(DEFAULT_ID);
			e.setAdvanceBookingDays(7);
			e.setMaxDurationMinutes(getOperatingSpanMinutes());
			e.setCancelAdvanceMinutes(30);
			e.setUpdateTime(new Date());
			this.save(e);
		}
		return e;
	}
}
