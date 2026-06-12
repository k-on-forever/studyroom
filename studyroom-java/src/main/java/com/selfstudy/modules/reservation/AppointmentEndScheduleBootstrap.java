package com.selfstudy.modules.reservation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.service.BasAppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时为进行中的预约补登记「时段结束」调度（含 Redis 清空后重启）。
 */
@Component
@ConditionalOnProperty(name = "study.reservation.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class AppointmentEndScheduleBootstrap {

	private static final Logger log = LoggerFactory.getLogger(AppointmentEndScheduleBootstrap.class);

	private final BasAppointmentService appointmentService;
	private final AppointmentEndRedisService sessionEnd;
	private final ReservationProperties reservationProperties;

	public AppointmentEndScheduleBootstrap(BasAppointmentService appointmentService,
			AppointmentEndRedisService sessionEnd,
			ReservationProperties reservationProperties) {
		this.appointmentService = appointmentService;
		this.sessionEnd = sessionEnd;
		this.reservationProperties = reservationProperties;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		if (!reservationProperties.isAutoEndOnSlotFinish()) {
			return;
		}
		try {
			List<BasAppointmentEntity> rows = appointmentService.list(new LambdaQueryWrapper<BasAppointmentEntity>()
					.in(BasAppointmentEntity::getSeatState, 0, 1));
			int n = 0;
			for (BasAppointmentEntity r : rows) {
				if (r.getId() != null && r.getBizDate() != null && r.getSlotEnd() != null) {
					sessionEnd.scheduleAtSlotEnd(r.getId(), r.getBizDate(), r.getSlotEnd());
					n++;
				}
			}
			if (n > 0) {
				log.info("Re-scheduled {} active appointment(s) for auto end on slot finish", n);
			}
		} catch (Exception e) {
			log.warn("Appointment end bootstrap skipped: {}", e.getMessage());
		}
	}
}
