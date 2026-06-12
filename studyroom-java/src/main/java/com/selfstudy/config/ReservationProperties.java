package com.selfstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 座位时间槽与信用分相关配置。
 */
@Component
@ConfigurationProperties(prefix = "study.reservation")
public class ReservationProperties {

	/** 每个时间槽分钟数（须与自习室 slot_step_minutes、小程序栅格一致） */
	private int slotMinutes = 10;
	/** 单次预约最短时长（分钟），默认 60（1 小时） */
	private int minBookingMinutes = 60;
	/** 营业开始，如 08:00 */
	private String dayStart = "08:00";
	/** 营业结束，与 application.yml study.reservation.day-end 一致（默认 22:30） */
	private String dayEnd = "22:30";
	/** 预约后未签到宽限期（分钟），到期写入 ZSET 由调度释放 */
	private int signInGraceMinutes = 15;
	/** 是否启用信用分（关闭后不校验、不扣分） */
	private boolean creditEnabled = false;
	/** 低于该分数禁止预约（仅 creditEnabled=true 时生效） */
	private int minCreditScore = 60;
	/** 爽约扣分（仅 creditEnabled=true 时生效） */
	private int noShowPenalty = 10;
	/** 新用户默认信用分 */
	private int initialCreditScore = 100;
	/** 未签到是否记为爽约并扣分；false 时仅释放座位并标记已取消 */
	private boolean noShowPenaltyEnabled = false;
	/** 预约时段结束后自动签退/释放（待签到→已取消，使用中→已完成） */
	private boolean autoEndOnSlotFinish = true;
	/** 门店授权（白名单专属）预约：不因未签到宽限期提前释放，仅到期自动释放 */
	private boolean skipNoShowForStoreAuth = true;
	/** 分布式锁等待秒数 */
	private int lockWaitSeconds = 3;
	/** 分布式锁租约秒数 */
	private int lockLeaseSeconds = 8;

	public int getSlotMinutes() {
		return slotMinutes;
	}

	public void setSlotMinutes(int slotMinutes) {
		this.slotMinutes = slotMinutes;
	}

	public int getMinBookingMinutes() {
		return minBookingMinutes;
	}

	public void setMinBookingMinutes(int minBookingMinutes) {
		this.minBookingMinutes = minBookingMinutes;
	}

	public String getDayStart() {
		return dayStart;
	}

	public void setDayStart(String dayStart) {
		this.dayStart = dayStart;
	}

	public String getDayEnd() {
		return dayEnd;
	}

	public void setDayEnd(String dayEnd) {
		this.dayEnd = dayEnd;
	}

	public int getSignInGraceMinutes() {
		return signInGraceMinutes;
	}

	public void setSignInGraceMinutes(int signInGraceMinutes) {
		this.signInGraceMinutes = signInGraceMinutes;
	}

	public boolean isCreditEnabled() {
		return creditEnabled;
	}

	public void setCreditEnabled(boolean creditEnabled) {
		this.creditEnabled = creditEnabled;
	}

	public boolean isNoShowPenaltyEnabled() {
		return noShowPenaltyEnabled;
	}

	public void setNoShowPenaltyEnabled(boolean noShowPenaltyEnabled) {
		this.noShowPenaltyEnabled = noShowPenaltyEnabled;
	}

	public boolean isAutoEndOnSlotFinish() {
		return autoEndOnSlotFinish;
	}

	public void setAutoEndOnSlotFinish(boolean autoEndOnSlotFinish) {
		this.autoEndOnSlotFinish = autoEndOnSlotFinish;
	}

	public boolean isSkipNoShowForStoreAuth() {
		return skipNoShowForStoreAuth;
	}

	public void setSkipNoShowForStoreAuth(boolean skipNoShowForStoreAuth) {
		this.skipNoShowForStoreAuth = skipNoShowForStoreAuth;
	}

	public int getMinCreditScore() {
		return minCreditScore;
	}

	public void setMinCreditScore(int minCreditScore) {
		this.minCreditScore = minCreditScore;
	}

	public int getNoShowPenalty() {
		return noShowPenalty;
	}

	public void setNoShowPenalty(int noShowPenalty) {
		this.noShowPenalty = noShowPenalty;
	}

	public int getInitialCreditScore() {
		return initialCreditScore;
	}

	public void setInitialCreditScore(int initialCreditScore) {
		this.initialCreditScore = initialCreditScore;
	}

	public int getLockWaitSeconds() {
		return lockWaitSeconds;
	}

	public void setLockWaitSeconds(int lockWaitSeconds) {
		this.lockWaitSeconds = lockWaitSeconds;
	}

	public int getLockLeaseSeconds() {
		return lockLeaseSeconds;
	}

	public void setLockLeaseSeconds(int lockLeaseSeconds) {
		this.lockLeaseSeconds = lockLeaseSeconds;
	}
}
