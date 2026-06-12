package com.selfstudy.modules.reservation;

import com.selfstudy.modules.bas.service.BasAppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 扫描 ZSET，对仍未签到的预约执行释放与扣分。
 */
@Component
@ConditionalOnProperty(name = "study.reservation.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class PendingSignInScheduler {

	private static final Logger log = LoggerFactory.getLogger(PendingSignInScheduler.class);

	private final PendingCheckInRedisService pending;
	private final BasAppointmentService appointmentService;

	public PendingSignInScheduler(PendingCheckInRedisService pending,
			BasAppointmentService appointmentService) {
		this.pending = pending;
		this.appointmentService = appointmentService;
	}

	@Scheduled(fixedDelayString = "${study.reservation.scheduler-ms:10000}")
	public void tick() {
		try {
			long now = System.currentTimeMillis();
			Set<String> due = pending.pollDueIds(now, 50);
			if (due == null || due.isEmpty()) {
				return;
			}
			for (String idStr : due) {
				try {
					Long id = Long.parseLong(idStr);
					appointmentService.processNoShowIfNeeded(id);
					pending.ackProcessed(idStr);
				} catch (Exception e) {
					log.warn("pending check-in handle failed id={}", idStr, e);
				}
			}
		} catch (Exception e) {
			log.debug("scheduler skip (redis/db unavailable): {}", e.getMessage());
		}
	}
}
