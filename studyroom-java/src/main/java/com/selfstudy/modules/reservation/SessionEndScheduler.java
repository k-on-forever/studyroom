package com.selfstudy.modules.reservation;

import com.selfstudy.config.ReservationProperties;
import com.selfstudy.modules.bas.service.BasAppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 扫描预约结束 ZSET：待签到/使用中且已过结束时刻的，自动释放座位并更新状态。
 */
@Component
@ConditionalOnProperty(name = "study.reservation.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class SessionEndScheduler {

	private static final Logger log = LoggerFactory.getLogger(SessionEndScheduler.class);

	private final AppointmentEndRedisService sessionEnd;
	private final BasAppointmentService appointmentService;
	private final ReservationProperties reservationProperties;

	public SessionEndScheduler(AppointmentEndRedisService sessionEnd,
			BasAppointmentService appointmentService,
			ReservationProperties reservationProperties) {
		this.sessionEnd = sessionEnd;
		this.appointmentService = appointmentService;
		this.reservationProperties = reservationProperties;
	}

	@Scheduled(fixedDelayString = "${study.reservation.scheduler-ms:10000}")
	public void tick() {
		if (!reservationProperties.isAutoEndOnSlotFinish()) {
			return;
		}
		try {
			long now = System.currentTimeMillis();
			Set<String> due = sessionEnd.pollDueIds(now, 50);
			if (due == null || due.isEmpty()) {
				return;
			}
			for (String idStr : due) {
				try {
					Long id = Long.parseLong(idStr);
					appointmentService.processSessionEndIfNeeded(id);
					sessionEnd.ackProcessed(idStr);
				} catch (Exception e) {
					log.warn("session end handle failed id={}", idStr, e);
				}
			}
		} catch (Exception e) {
			log.debug("session end scheduler skip: {}", e.getMessage());
		}
	}
}
