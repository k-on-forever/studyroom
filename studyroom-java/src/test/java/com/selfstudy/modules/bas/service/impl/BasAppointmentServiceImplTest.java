package com.selfstudy.modules.bas.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.selfstudy.common.exception.RRException;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.config.StudyMiniProperties;
import com.selfstudy.config.StudyWxPayProperties;
import com.selfstudy.modules.pay.WxPayBizService;
import com.selfstudy.modules.applet.vo.BasAppointmentVO;
import com.selfstudy.modules.bas.dao.BasFloorDao;
import com.selfstudy.modules.bas.dao.BasSeatDao;
import com.selfstudy.modules.bas.dao.BasStudyRoomDao;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.entity.BasFloorEntity;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import com.selfstudy.modules.bas.service.BasMembershipOrderService;
import com.selfstudy.modules.bas.service.BasSeatAccessRuleService;

import com.selfstudy.modules.credit.UserCreditRedisService;
import com.selfstudy.modules.reservation.AppointmentEndRedisService;
import com.selfstudy.modules.reservation.PendingCheckInRedisService;
import com.selfstudy.modules.reservation.SeatBitmapRedisService;
import com.selfstudy.modules.reservation.SeatBitmapRebuildService;
import com.selfstudy.modules.reservation.SeatReservationOrchestrator;
import com.selfstudy.modules.reservation.TimeSlotCodec;
import com.selfstudy.modules.sys.service.ReservationRuleConfigService;
import com.selfstudy.modules.user.service.TbUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasAppointmentServiceImplTest {

	@Mock
	private TimeSlotCodec timeSlotCodec;
	@Mock
	private SeatReservationOrchestrator orchestrator;
	@Mock
	private PendingCheckInRedisService pendingCheckIn;
	@Mock
	private AppointmentEndRedisService appointmentEnd;
	@Mock
	private UserCreditRedisService credit;
	@Mock
	private ReservationProperties reservationProperties;
	@Mock
	private SeatBitmapRedisService seatBitmapRedisService;
	@Mock
	private BasSeatDao basSeatDao;
	@Mock
	private BasStudyRoomDao basStudyRoomDao;
	@Mock
	private BasFloorDao basFloorDao;
	@Mock
	private ObjectProvider<com.selfstudy.modules.reservation.StudySeatLockService> studySeatLock;
	@Mock
	private ReservationRuleConfigService reservationRuleConfig;
	@Mock
	private BasMembershipOrderService basMembershipOrderService;
	@Mock
	private StudyMiniProperties studyMiniProperties;
	@Mock
	private TbUserService tbUserService;
	@Mock
	private BasSeatAccessRuleService basSeatAccessRuleService;
	@Mock
	private SeatBitmapRebuildService seatBitmapRebuildService;
	@Mock
	private StudyWxPayProperties studyWxPayProperties;
	@Mock
	private WxPayBizService wxPayBizService;

	@Spy
	@InjectMocks
	private BasAppointmentServiceImpl service;

	@Test
	void myAppointment_batchLoadsPlaceNames() {
		BasAppointmentEntity row = new BasAppointmentEntity();
		row.setId(1L);
		row.setUserId(5L);
		row.setSeatId(100L);
		doReturn(List.of(row)).when(service).list(any(Wrapper.class));

		BasSeatEntity seat = new BasSeatEntity();
		seat.setId(100L);
		seat.setSeatName("A-01");
		seat.setRoomId(200L);
		when(basSeatDao.selectByIds(any(Collection.class))).thenReturn(List.of(seat));

		BasStudyRoomEntity room = new BasStudyRoomEntity();
		room.setId(200L);
		room.setRoomName("自习室A");
		room.setFloorId(300L);
		when(basStudyRoomDao.selectByIds(any(Collection.class))).thenReturn(List.of(room));

		BasFloorEntity floor = new BasFloorEntity();
		floor.setId(300L);
		floor.setFloorName("1F");
		when(basFloorDao.selectByIds(any(Collection.class))).thenReturn(List.of(floor));

		List<BasAppointmentVO> list = service.myAppointment(5L);

		assertEquals(1, list.size());
		assertEquals("A-01", list.get(0).getSName());
		assertEquals("自习室A", list.get(0).getRoomName());
		assertEquals("1F", list.get(0).getFloor());
		verify(basSeatDao).selectByIds(any(Collection.class));
	}

	@Test
	void getMineById_deniesOtherUser() {
		BasAppointmentEntity row = new BasAppointmentEntity();
		row.setId(2L);
		row.setUserId(10L);
		doReturn(row).when(service).getById(2L);
		assertNull(service.getMineById(2L, 99L));
	}

	@Test
	void cancel_throwsWhenNotFound() {
		doReturn(null).when(service).getById(99L);
		assertThrows(RRException.class, () -> service.cancel(99L, 5L));
	}

	@Test
	void processSessionEndIfNeeded_completesInUseWhenPastEnd() {
		when(reservationProperties.isAutoEndOnSlotFinish()).thenReturn(true);
		BasAppointmentEntity row = new BasAppointmentEntity();
		row.setId(7L);
		row.setSeatState(1);
		row.setSeatId(10L);
		row.setBizDate("2020-01-01");
		row.setSlotStart(0);
		row.setSlotEnd(1);
		doReturn(row).when(service).getById(7L);
		when(appointmentEnd.computeSlotEndMs("2020-01-01", 1)).thenReturn(1L);
		doReturn(true).when(service).updateById(row);

		service.processSessionEndIfNeeded(7L);

		assertEquals(3, row.getSeatState());
		assertNotNull(row.getStudyEndAt());
	}
}
