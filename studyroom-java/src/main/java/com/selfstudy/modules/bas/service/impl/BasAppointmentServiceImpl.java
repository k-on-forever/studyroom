package com.selfstudy.modules.bas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.common.base.PageResult;
import com.selfstudy.common.exception.RRException;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.config.StudyMiniProperties;
import com.selfstudy.config.StudyWxPayProperties;
import com.selfstudy.modules.pay.WxPayBizService;
import com.selfstudy.modules.applet.dto.AppointmentBookResult;
import com.selfstudy.modules.applet.dto.save.BasAppointmentSaveDTO;
import com.selfstudy.modules.applet.vo.BasAppointmentVO;
import com.selfstudy.modules.bas.dao.BasAppointmentDao;
import com.selfstudy.modules.bas.dao.BasFloorDao;
import com.selfstudy.modules.bas.dao.BasSeatDao;
import com.selfstudy.modules.bas.dao.BasStudyRoomDao;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.entity.BasFloorEntity;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import com.selfstudy.modules.bas.service.BasAppointmentService;
import com.selfstudy.modules.bas.service.BasMembershipOrderService;
import com.selfstudy.modules.bas.service.BasSeatAccessRuleService;

import com.selfstudy.modules.bas.vo.BasAppointmentAdminVO;
import com.selfstudy.modules.user.entity.TbUserEntity;
import com.selfstudy.modules.user.service.TbUserService;
import com.selfstudy.modules.reservation.AppointmentEndRedisService;
import com.selfstudy.modules.reservation.PendingCheckInRedisService;
import com.selfstudy.modules.reservation.SeatBitmapRebuildService;
import com.selfstudy.modules.reservation.SeatDayDisplay;
import com.selfstudy.modules.reservation.SeatBitmapRedisService;
import com.selfstudy.modules.reservation.SeatReservationOrchestrator;
import com.selfstudy.modules.reservation.StudySeatLockService;
import com.selfstudy.modules.reservation.TimeSlotCodec;
import com.selfstudy.modules.sys.service.ReservationRuleConfigService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BasAppointmentServiceImpl extends ServiceImpl<BasAppointmentDao, BasAppointmentEntity>
		implements BasAppointmentService {

	private final TimeSlotCodec timeSlotCodec;
	private final SeatReservationOrchestrator orchestrator;
	private final PendingCheckInRedisService pendingCheckIn;
	private final AppointmentEndRedisService appointmentEnd;
	private final ReservationProperties reservationProperties;
	private final SeatBitmapRedisService seatBitmapRedisService;
	private final BasSeatDao basSeatDao;
	private final BasStudyRoomDao basStudyRoomDao;
	private final BasFloorDao basFloorDao;
	private final ObjectProvider<StudySeatLockService> studySeatLock;
	private final ReservationRuleConfigService reservationRuleConfig;
	private final BasMembershipOrderService basMembershipOrderService;
	private final StudyMiniProperties studyMiniProperties;
	private final TbUserService tbUserService;
	private final BasSeatAccessRuleService basSeatAccessRuleService;
	private final SeatBitmapRebuildService seatBitmapRebuildService;
	private final StudyWxPayProperties studyWxPayProperties;
	private final WxPayBizService wxPayBizService;
	private final com.selfstudy.modules.bas.dao.BasUserHourWalletDao basUserHourWalletDao;

	public BasAppointmentServiceImpl(TimeSlotCodec timeSlotCodec,
			SeatReservationOrchestrator orchestrator,
			PendingCheckInRedisService pendingCheckIn,
			AppointmentEndRedisService appointmentEnd,
			ReservationProperties reservationProperties,
			SeatBitmapRedisService seatBitmapRedisService,
			BasSeatDao basSeatDao,
			BasStudyRoomDao basStudyRoomDao,
			BasFloorDao basFloorDao,
			ObjectProvider<StudySeatLockService> studySeatLock,
			ReservationRuleConfigService reservationRuleConfig,
			BasMembershipOrderService basMembershipOrderService,
			StudyMiniProperties studyMiniProperties,
			TbUserService tbUserService,
			BasSeatAccessRuleService basSeatAccessRuleService,
			SeatBitmapRebuildService seatBitmapRebuildService,
			StudyWxPayProperties studyWxPayProperties,
			WxPayBizService wxPayBizService,
			com.selfstudy.modules.bas.dao.BasUserHourWalletDao basUserHourWalletDao) {
		this.timeSlotCodec = timeSlotCodec;
		this.orchestrator = orchestrator;
		this.pendingCheckIn = pendingCheckIn;
		this.appointmentEnd = appointmentEnd;
		this.reservationProperties = reservationProperties;
		this.seatBitmapRedisService = seatBitmapRedisService;
		this.basSeatDao = basSeatDao;
		this.basStudyRoomDao = basStudyRoomDao;
		this.basFloorDao = basFloorDao;
		this.studySeatLock = studySeatLock;
		this.reservationRuleConfig = reservationRuleConfig;
		this.basMembershipOrderService = basMembershipOrderService;
		this.studyMiniProperties = studyMiniProperties;
		this.tbUserService = tbUserService;
		this.basSeatAccessRuleService = basSeatAccessRuleService;
		this.seatBitmapRebuildService = seatBitmapRebuildService;
		this.studyWxPayProperties = studyWxPayProperties;
		this.wxPayBizService = wxPayBizService;
		this.basUserHourWalletDao = basUserHourWalletDao;
	}

	/** 小程序可不传姓名/手机/班级，落库时用登录用户资料补全 */
	private void fillContactFromUser(BasAppointmentSaveDTO saveDTO, Long userId) {
		if (userId == null) {
			return;
		}
		TbUserEntity u = tbUserService.getById(userId);
		if (u == null) {
			return;
		}
		if (!StringUtils.hasText(saveDTO.getSeatPhone())) {
			saveDTO.setSeatPhone(StringUtils.hasText(u.getMobile()) ? u.getMobile().trim() : "--");
		}
		if (!StringUtils.hasText(saveDTO.getSeatName())) {
			String nm = StringUtils.hasText(u.getName()) ? u.getName().trim()
					: (StringUtils.hasText(u.getUsername()) ? u.getUsername().trim() : "");
			saveDTO.setSeatName(StringUtils.hasText(nm) ? nm : "用户" + userId);
		}
		if (!StringUtils.hasText(saveDTO.getSeatClass())) {
			saveDTO.setSeatClass("--");
		}
	}

	@Override
	public Map<String, Object> quoteAppointment(BasAppointmentSaveDTO saveDTO, Long userId) {
		BasSeatEntity seat = basSeatDao.selectById(saveDTO.getSeatId());
		if (seat == null) {
			throw new RRException("座位不存在");
		}
		BasStudyRoomEntity room = seat.getRoomId() == null ? null : basStudyRoomDao.selectById(seat.getRoomId());
		TimeSlotCodec.ParsedSeatDay parsed = timeSlotCodec.parse(saveDTO.getSeatDay(), room);
		basSeatAccessRuleService.assertUserMayBook(userId, saveDTO.getSeatId(), parsed);
		int slotCount = parsed.slotEndExclusive - parsed.slotStartInclusive;
		boolean member = basMembershipOrderService.hasActiveMembership(userId);
		int slotM = reservationProperties.getSlotMinutes();
		int totalMinutes = slotCount * slotM;
		// 计费小时数：向上取整，不满 1h 按 1h，1h+ 按 2h，以此类推
		int billableHours = computeBillableHours(totalMinutes);
		BigDecimal amount = BigDecimal.ZERO;
		if (!member) {
			double p = studyMiniProperties.getPerSlotPriceYuan();
			if (p > 0 && billableHours > 0) {
				amount = BigDecimal.valueOf(p).multiply(BigDecimal.valueOf(billableHours));
			}
		}
		Map<String, Object> m = new HashMap<>();
		m.put("memberFree", member);
		m.put("slotCount", slotCount);
		m.put("amountYuan", amount);
		return m;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AppointmentBookResult appointment(BasAppointmentSaveDTO saveDTO, Long userId) {
		fillContactFromUser(saveDTO, userId);
		BasSeatEntity seat = basSeatDao.selectById(saveDTO.getSeatId());
		if (seat == null) {
			return AppointmentBookResult.fail("座位不存在");
		}
		if (seat.getLocked() != null && seat.getLocked() == 1) {
			return AppointmentBookResult.fail("座位不可用");
		}
		BasStudyRoomEntity room = seat.getRoomId() == null ? null : basStudyRoomDao.selectById(seat.getRoomId());
		TimeSlotCodec.ParsedSeatDay parsed = timeSlotCodec.parse(saveDTO.getSeatDay(), room);
		int max = seatBitmapRedisService.maxSlots();
		if (parsed.slotEndExclusive > max || parsed.slotStartInclusive < 0) {
			return AppointmentBookResult.fail("时段不合法");
		}

		String accessDeny = basSeatAccessRuleService.bookDenyReason(userId, saveDTO.getSeatId(), parsed);
		if (accessDeny != null) {
			return AppointmentBookResult.fail(accessDeny);
		}

		int slotCount = parsed.slotEndExclusive - parsed.slotStartInclusive;
		boolean member = basMembershipOrderService.hasActiveMembership(userId);
		int slotM = reservationProperties.getSlotMinutes();
		int totalMinutes = slotCount * slotM;
		int billableHours = computeBillableHours(totalMinutes);
		BigDecimal payAmount = BigDecimal.ZERO;
		if (!member) {
			double p = studyMiniProperties.getPerSlotPriceYuan();
			if (p > 0 && billableHours > 0) {
				payAmount = BigDecimal.valueOf(p).multiply(BigDecimal.valueOf(billableHours));
			}
		}
		if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
			if (Boolean.TRUE.equals(saveDTO.getSimulatePaidNonMember())) {
				if (!studyWxPayProperties.isMockMode()) {
					return AppointmentBookResult.fail("请使用微信支付，勿使用模拟支付参数");
				}
			} else if (StringUtils.hasText(saveDTO.getWxPayOutTradeNo())) {
				try {
					wxPayBizService.assertAppointmentPaid(userId, saveDTO.getWxPayOutTradeNo().trim(), payAmount);
				} catch (RRException e) {
					return AppointmentBookResult.fail(e.getMessage());
				}
			} else {
				return AppointmentBookResult.needPay(payAmount, slotCount);
			}
		}
		int payStatus = member ? 0 : (payAmount.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);

		StudySeatLockService hold = studySeatLock.getIfAvailable();
		String holdSegment = parsed.bizDate + ":" + parsed.slotStartInclusive + ":" + parsed.slotEndExclusive;
		if (hold != null && !hold.tryHold(saveDTO.getSeatId(), holdSegment, userId)) {
			return AppointmentBookResult.fail("座位抢占中，请稍后重试");
		}
		try {
			boolean ok;
			try {
				ok = orchestrator.tryReserve(saveDTO.getSeatId(), parsed.bizDate,
						parsed.slotStartInclusive, parsed.slotEndExclusive);
			} catch (Exception e) {
				throw new IllegalStateException("预约加锁失败", e);
			}
			if (!ok) {
				String occ = SeatDayDisplay.withOccupiedPrefix(saveDTO.getSeatDay());
				return AppointmentBookResult.fail(
						StringUtils.hasText(occ)
								? "该时段已被占用或不可预约，" + occ
								: "该时段已被占用或不可预约");
			}
			// 数据库层面防超售检查：确认同一座位同一时段没有有效预约
			Long existingCount = count(new LambdaQueryWrapper<BasAppointmentEntity>()
					.eq(BasAppointmentEntity::getSeatId, saveDTO.getSeatId())
					.eq(BasAppointmentEntity::getBizDate, parsed.bizDate)
					.eq(BasAppointmentEntity::getSlotStart, parsed.slotStartInclusive)
					.eq(BasAppointmentEntity::getSlotEnd, parsed.slotEndExclusive)
					.in(BasAppointmentEntity::getSeatState, 0, 1));
			if (existingCount != null && existingCount > 0) {
				orchestrator.release(saveDTO.getSeatId(), parsed.bizDate,
						parsed.slotStartInclusive, parsed.slotEndExclusive);
				return AppointmentBookResult.fail("该时段已被预约");
			}

			BasAppointmentEntity e = new BasAppointmentEntity();
			e.setSeatId(saveDTO.getSeatId());
			e.setUserId(userId);
			e.setSeatPhone(saveDTO.getSeatPhone());
			e.setSeatName(saveDTO.getSeatName());
			e.setSeatClass(saveDTO.getSeatClass());
			e.setSeatDay(parsed.display);
			e.setBizDate(parsed.bizDate);
			e.setSlotStart(parsed.slotStartInclusive);
			e.setSlotEnd(parsed.slotEndExclusive);
			e.setSeatState(0);
			e.setPayAmount(payAmount);
			e.setPayStatus(payStatus);
			e.setCreateTime(new Date());
			boolean saved;
			try {
				saved = save(e);
			} catch (Throwable t) {
				// 位图已在 tryReserve 中占用；保存抛错时必须释放，否则 Redis 与库不一致（界面仍像「空位」）
				orchestrator.release(saveDTO.getSeatId(), parsed.bizDate,
						parsed.slotStartInclusive, parsed.slotEndExclusive);
				throw t;
			}
			if (!saved) {
				orchestrator.release(saveDTO.getSeatId(), parsed.bizDate,
						parsed.slotStartInclusive, parsed.slotEndExclusive);
				return AppointmentBookResult.fail("保存失败");
			}
			boolean storeAuth = reservationProperties.isSkipNoShowForStoreAuth()
					&& basSeatAccessRuleService.isStoreAuthBooking(userId, saveDTO.getSeatId(), parsed);
			if (!storeAuth) {
				pendingCheckIn.scheduleAtSlotStart(e.getId(), parsed.bizDate, parsed.slotStartInclusive);
			}
			if (reservationProperties.isAutoEndOnSlotFinish()) {
				appointmentEnd.scheduleAtSlotEnd(e.getId(), parsed.bizDate, parsed.slotEndExclusive);
			}
			return AppointmentBookResult.success();
		} finally {
			if (hold != null) {
				hold.releaseHold(saveDTO.getSeatId(), holdSegment, userId);
			}
		}
	}

	@Override
	public List<BasAppointmentVO> myAppointment(Long userId) {
		List<BasAppointmentEntity> rows = list(new LambdaQueryWrapper<BasAppointmentEntity>()
				.eq(BasAppointmentEntity::getUserId, userId)
				.orderByDesc(BasAppointmentEntity::getCreateTime));
		AppointmentPlaceLookup lookup = buildPlaceLookup(rows);
		List<BasAppointmentVO> out = new ArrayList<>(rows.size());
		for (BasAppointmentEntity r : rows) {
			out.add(toVo(r, lookup));
		}
		return out;
	}

	@Override
	public BasAppointmentVO getMineById(Long id, Long userId) {
		BasAppointmentEntity r = getById(id);
		if (r == null || userId == null || !userId.equals(r.getUserId())) {
			return null;
		}
		return toVo(r, buildPlaceLookup(Collections.singletonList(r)));
	}

	private BasAppointmentVO toVo(BasAppointmentEntity r, AppointmentPlaceLookup lookup) {
		BasAppointmentVO vo = new BasAppointmentVO();
		vo.setId(r.getId());
		vo.setSeatId(r.getSeatId());
		vo.setUserId(r.getUserId());
		vo.setSeatPhone(r.getSeatPhone());
		vo.setSeatName(r.getSeatName());
		vo.setSeatClass(r.getSeatClass());
		vo.setSeatState(r.getSeatState());
		vo.setSeatDay(r.getSeatDay());
		vo.setPayAmount(r.getPayAmount());
		vo.setPayStatus(r.getPayStatus());
		vo.setCheckInAt(r.getCheckInAt() == null ? null : r.getCheckInAt().getTime());
		vo.setStudyEndAt(r.getStudyEndAt() == null ? null : r.getStudyEndAt().getTime());
		applyPlaceToVo(vo, lookup.seat(r.getSeatId()), lookup);
		return vo;
	}

	private void applyPlaceToVo(BasAppointmentVO vo, BasSeatEntity seat, AppointmentPlaceLookup lookup) {
		if (seat == null) {
			return;
		}
		vo.setSName(seat.getSeatName());
		BasStudyRoomEntity room = lookup.room(seat.getRoomId());
		if (room == null) {
			return;
		}
		vo.setRoomName(room.getRoomName());
		BasFloorEntity floor = lookup.floor(room.getFloorId());
		if (floor != null) {
			vo.setFloor(floor.getFloorName());
		}
	}

	private boolean isStoreAuthAppointment(BasAppointmentEntity r) {
		if (!reservationProperties.isSkipNoShowForStoreAuth() || r == null) {
			return false;
		}
		if (r.getUserId() == null || r.getSeatId() == null
				|| !StringUtils.hasText(r.getBizDate()) || r.getSlotStart() == null || r.getSlotEnd() == null) {
			return false;
		}
		try {
			TimeSlotCodec.ParsedSeatDay parsed = new TimeSlotCodec.ParsedSeatDay(
					r.getBizDate().trim(), r.getSlotStart(), r.getSlotEnd(),
					r.getSeatDay() != null ? r.getSeatDay() : r.getBizDate());
			return basSeatAccessRuleService.isStoreAuthBooking(r.getUserId(), r.getSeatId(), parsed);
		} catch (Exception ignored) {
			return false;
		}
	}

	/** 预约时段开始时刻（本地时区） */
	private LocalDateTime appointmentStartAt(BasAppointmentEntity r) {
		int slotM = reservationProperties.getSlotMinutes();
		int dsMin = LocalTime.parse(reservationProperties.getDayStart().trim()).toSecondOfDay() / 60;
		int startMin = dsMin + r.getSlotStart() * slotM;
		LocalTime t = LocalTime.ofSecondOfDay((long) startMin * 60L);
		return LocalDate.parse(r.getBizDate().trim(), DateTimeFormatter.ISO_LOCAL_DATE).atTime(t);
	}

	/**
	 * 注册事务提交后执行的 Redis 清理操作，确保 Redis 与 DB 一致性。
	 * 如果事务回滚，Redis 操作不会执行。
	 */
	private void afterCommit(Runnable redisOperation) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				redisOperation.run();
			}
		});
	}

	@Override
	public int rebuildSeatBitmapsFromDb() {
		return seatBitmapRebuildService.rebuildFromDatabase();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean cancel(Long id, Long userId) {
		BasAppointmentEntity r = getById(id);
		if (r == null) {
			throw new RRException("预约不存在");
		}
		if (userId == null || !userId.equals(r.getUserId())) {
			throw new RRException("无权取消该预约");
		}
		if (r.getSeatState() == null || r.getSeatState() != 0) {
			throw new RRException("仅待签到预约可取消，使用中请签退");
		}
		if (r.getBizDate() == null || r.getSlotStart() == null) {
			throw new RRException("预约时段数据异常，请联系门店");
		}
		LocalDateTime apptStart = appointmentStartAt(r);
		LocalDateTime apptEnd = apptStart.plusMinutes((long)(r.getSlotEnd() - r.getSlotStart()) * reservationProperties.getSlotMinutes());
		if (!LocalDateTime.now().isBefore(apptStart)) {
			throw new RRException("超过了预约的开始时间，无法取消");
		}

		// 非会员按次付费：取消时返还时长到小时钱包
		if (r.getPayStatus() != null && r.getPayStatus() == 1 && r.getPayAmount() != null && r.getPayAmount().compareTo(BigDecimal.ZERO) > 0) {
			int slotMinutes = reservationProperties.getSlotMinutes();
			int totalMinutes = (r.getSlotEnd() - r.getSlotStart()) * slotMinutes;
			BigDecimal refundHours = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
			com.selfstudy.modules.bas.entity.BasUserHourWalletEntity wallet = basUserHourWalletDao.selectOne(
					new LambdaQueryWrapper<com.selfstudy.modules.bas.entity.BasUserHourWalletEntity>()
							.eq(com.selfstudy.modules.bas.entity.BasUserHourWalletEntity::getUserId, r.getUserId()));
			if (wallet == null) {
				wallet = new com.selfstudy.modules.bas.entity.BasUserHourWalletEntity();
				wallet.setUserId(r.getUserId());
				wallet.setBalanceHours(refundHours);
				wallet.setUpdateTime(new Date());
				basUserHourWalletDao.insert(wallet);
			} else {
				wallet.setBalanceHours(wallet.getBalanceHours().add(refundHours));
				wallet.setUpdateTime(new Date());
				basUserHourWalletDao.updateById(wallet);
			}
		}

		retryUpdateState(id, fresh -> {
			if (fresh.getSeatState() == null || fresh.getSeatState() != 0) {
				throw new RRException("仅待签到预约可取消，使用中请签退");
			}
			fresh.setSeatState(2);
		}, "取消失败，请稍后重试");

		// Redis 操作移到事务提交后，避免事务回滚时 Redis 与 DB 不一致
		final Long seatId = r.getSeatId();
		final String bizDate = r.getBizDate();
		final Integer slotStart = r.getSlotStart();
		final Integer slotEnd = r.getSlotEnd();
		afterCommit(() -> {
			pendingCheckIn.cancelSchedule(id);
			appointmentEnd.cancelSchedule(id);
			orchestrator.release(seatId, bizDate, slotStart, slotEnd);
		});
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean over(Long id, Long userId) {
		BasAppointmentEntity r = getById(id);
		if (r == null) {
			throw new RRException("预约不存在");
		}
		if (userId == null || !userId.equals(r.getUserId())) {
			throw new RRException("无权操作该预约");
		}
		if (r.getSeatState() == null || r.getSeatState() != 1) {
			throw new RRException("请先签到后再签退");
		}
		retryUpdateState(id, fresh -> {
			if (fresh.getSeatState() == null || fresh.getSeatState() != 1) {
				throw new RRException("请先签到后再签退");
			}
			fresh.setSeatState(3);
			if (fresh.getStudyEndAt() == null) {
				fresh.setStudyEndAt(new Date());
			}
		}, "签退失败，请稍后重试");

		// Redis 操作移到事务提交后
		final Long seatId = r.getSeatId();
		final String bizDate = r.getBizDate();
		final Integer slotStart = r.getSlotStart();
		final Integer slotEnd = r.getSlotEnd();
		afterCommit(() -> {
			pendingCheckIn.cancelSchedule(id);
			appointmentEnd.cancelSchedule(id);
			orchestrator.release(seatId, bizDate, slotStart, slotEnd);
		});
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean signIn(Long appointmentId, Long userId) {
		BasAppointmentEntity r = getById(appointmentId);
		if (r == null) {
			throw new RRException("预约不存在");
		}
		if (userId == null || !userId.equals(r.getUserId())) {
			throw new RRException("无权签到该预约");
		}
		if (r.getSeatState() == null || r.getSeatState() != 0) {
			throw new RRException("当前状态不可签到（可能已签到、已取消或已完成）");
		}
		if (r.getBizDate() == null || r.getSlotStart() == null) {
			throw new RRException("预约时段数据异常，请联系门店");
		}
		LocalDateTime apptStart = appointmentStartAt(r);
		if (LocalDateTime.now().isBefore(apptStart)) {
			throw new RRException(
					"未到预约开始时间，请于 " + apptStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " 后再签到");
		}
		retryUpdateState(appointmentId, fresh -> {
			if (fresh.getSeatState() == null || fresh.getSeatState() != 0) {
				throw new RRException("当前状态不可签到（可能已签到、已取消或已完成）");
			}
			fresh.setSeatState(1);
			if (fresh.getCheckInAt() == null) {
				fresh.setCheckInAt(new Date());
			}
		}, "签到失败，请稍后重试");

		// Redis 操作移到事务提交后
		afterCommit(() -> pendingCheckIn.cancelSchedule(appointmentId));
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void processNoShowIfNeeded(Long appointmentId) {
		BasAppointmentEntity r = getById(appointmentId);
		if (r == null || r.getSeatState() == null || r.getSeatState() != 0) {
			return;
		}
		if (isStoreAuthAppointment(r)) {
			return;
		}
		r.setSeatState(2);
		updateById(r);

		// Redis 操作移到事务提交后
		final Long seatId = r.getSeatId();
		final String bizDate = r.getBizDate();
		final Integer slotStart = r.getSlotStart();
		final Integer slotEnd = r.getSlotEnd();
		afterCommit(() -> {
			appointmentEnd.cancelSchedule(appointmentId);
			orchestrator.release(seatId, bizDate, slotStart, slotEnd);
		});
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void processSessionEndIfNeeded(Long appointmentId) {
		if (!reservationProperties.isAutoEndOnSlotFinish()) {
			return;
		}
		BasAppointmentEntity r = getById(appointmentId);
		if (r == null || r.getSeatState() == null) {
			return;
		}
		int st = r.getSeatState();
		if (st != 0 && st != 1) {
			afterCommit(() -> appointmentEnd.cancelSchedule(appointmentId));
			return;
		}
		if (r.getBizDate() == null || r.getSlotEnd() == null || r.getSlotStart() == null) {
			return;
		}
		long endMs = appointmentEnd.computeSlotEndMs(r.getBizDate(), r.getSlotEnd());
		if (System.currentTimeMillis() < endMs) {
			return;
		}
		if (st == 1) {
			r.setSeatState(3);
			if (r.getStudyEndAt() == null) {
				r.setStudyEndAt(new Date());
			}
		} else {
			r.setSeatState(2);
		}
		updateById(r);

		// Redis 操作移到事务提交后
		final Long seatId = r.getSeatId();
		final String bizDate = r.getBizDate();
		final Integer slotStart = r.getSlotStart();
		final Integer slotEnd = r.getSlotEnd();
		afterCommit(() -> {
			pendingCheckIn.cancelSchedule(appointmentId);
			appointmentEnd.cancelSchedule(appointmentId);
			orchestrator.release(seatId, bizDate, slotStart, slotEnd);
		});
	}

	@Override
	public PageResult<BasAppointmentAdminVO> adminPage(long page, long limit, String bizDate, Integer seatState,
			String keyword) {
		long p = Math.max(1L, page);
		long l = Math.min(100L, Math.max(1L, limit));
		LambdaQueryWrapper<BasAppointmentEntity> w = new LambdaQueryWrapper<>();
		if (StringUtils.hasText(bizDate)) {
			w.eq(BasAppointmentEntity::getBizDate, bizDate.trim());
		}
		if (seatState != null) {
			w.eq(BasAppointmentEntity::getSeatState, seatState);
		}
		if (StringUtils.hasText(keyword)) {
			String k = keyword.trim();
			w.and(q -> q.like(BasAppointmentEntity::getSeatPhone, k)
					.or().like(BasAppointmentEntity::getSeatName, k)
					.or().like(BasAppointmentEntity::getSeatDay, k)
					.or().like(BasAppointmentEntity::getBizDate, k));
		}
		w.orderByDesc(BasAppointmentEntity::getCreateTime);
		Page<BasAppointmentEntity> pg = page(new Page<>(p, l), w);
		AppointmentPlaceLookup lookup = buildPlaceLookup(pg.getRecords());
		List<BasAppointmentAdminVO> vos = new ArrayList<>(pg.getRecords().size());
		for (BasAppointmentEntity r : pg.getRecords()) {
			vos.add(toAdminVo(r, lookup));
		}
		return new PageResult<>(vos, pg.getTotal());
	}

	@Override
	public List<BasAppointmentAdminVO> adminExport(String bizDate, Integer seatState, String keyword) {
		LambdaQueryWrapper<BasAppointmentEntity> w = new LambdaQueryWrapper<>();
		if (StringUtils.hasText(bizDate)) {
			w.eq(BasAppointmentEntity::getBizDate, bizDate.trim());
		}
		if (seatState != null) {
			w.eq(BasAppointmentEntity::getSeatState, seatState);
		}
		if (StringUtils.hasText(keyword)) {
			String k = keyword.trim();
			w.and(q -> q.like(BasAppointmentEntity::getSeatPhone, k)
					.or().like(BasAppointmentEntity::getSeatName, k)
					.or().like(BasAppointmentEntity::getSeatDay, k)
					.or().like(BasAppointmentEntity::getBizDate, k));
		}
		w.orderByDesc(BasAppointmentEntity::getCreateTime);
		w.last("LIMIT 5000");
		List<BasAppointmentEntity> rows = list(w);
		AppointmentPlaceLookup lookup = buildPlaceLookup(rows);
		List<BasAppointmentAdminVO> vos = new ArrayList<>(rows.size());
		for (BasAppointmentEntity r : rows) {
			vos.add(toAdminVo(r, lookup));
		}
		return vos;
	}

	private BasAppointmentAdminVO toAdminVo(BasAppointmentEntity r, AppointmentPlaceLookup lookup) {
		BasAppointmentAdminVO vo = new BasAppointmentAdminVO();
		vo.setId(r.getId());
		vo.setSeatId(r.getSeatId());
		vo.setUserId(r.getUserId());
		vo.setSeatPhone(r.getSeatPhone());
		vo.setSeatName(r.getSeatName());
		vo.setSeatClass(r.getSeatClass());
		vo.setSeatState(r.getSeatState());
		vo.setSeatDay(r.getSeatDay());
		vo.setBizDate(r.getBizDate());
		vo.setSlotStart(r.getSlotStart());
		vo.setSlotEnd(r.getSlotEnd());
		vo.setCreateTime(r.getCreateTime());
		vo.setCheckInAt(r.getCheckInAt());
		vo.setStudyEndAt(r.getStudyEndAt());
		BasSeatEntity seat = lookup.seat(r.getSeatId());
		if (seat != null) {
			vo.setSeatLabel(seat.getSeatName());
			BasStudyRoomEntity room = lookup.room(seat.getRoomId());
			if (room != null) {
				vo.setRoomName(room.getRoomName());
				BasFloorEntity floor = lookup.floor(room.getFloorId());
				if (floor != null) {
					vo.setFloor(floor.getFloorName());
				}
			}
		}
		return vo;
	}

	/** 批量预加载座位/自习室/楼层，避免列表接口 N+1 */
	private AppointmentPlaceLookup buildPlaceLookup(List<BasAppointmentEntity> rows) {
		if (rows == null || rows.isEmpty()) {
			return AppointmentPlaceLookup.empty();
		}
		Set<Long> seatIds = new HashSet<>();
		for (BasAppointmentEntity r : rows) {
			if (r.getSeatId() != null) {
				seatIds.add(r.getSeatId());
			}
		}
		Map<Long, BasSeatEntity> seats = seatIds.isEmpty()
				? Collections.emptyMap()
				: indexById(basSeatDao.selectByIds(seatIds), BasSeatEntity::getId);
		Set<Long> roomIds = new HashSet<>();
		for (BasSeatEntity seat : seats.values()) {
			if (seat.getRoomId() != null) {
				roomIds.add(seat.getRoomId());
			}
		}
		Map<Long, BasStudyRoomEntity> rooms = roomIds.isEmpty()
				? Collections.emptyMap()
				: indexById(basStudyRoomDao.selectByIds(roomIds), BasStudyRoomEntity::getId);
		Set<Long> floorIds = new HashSet<>();
		for (BasStudyRoomEntity room : rooms.values()) {
			if (room.getFloorId() != null) {
				floorIds.add(room.getFloorId());
			}
		}
		Map<Long, BasFloorEntity> floors = floorIds.isEmpty()
				? Collections.emptyMap()
				: indexById(basFloorDao.selectByIds(floorIds), BasFloorEntity::getId);
		return new AppointmentPlaceLookup(seats, rooms, floors);
	}

	private static <T> Map<Long, T> indexById(List<T> list, java.util.function.Function<T, Long> idFn) {
		if (list == null || list.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, T> map = new HashMap<>(list.size());
		for (T item : list) {
			Long id = idFn.apply(item);
			if (id != null) {
				map.put(id, item);
			}
		}
		return map;
	}

	private static final class AppointmentPlaceLookup {
		private final Map<Long, BasSeatEntity> seats;
		private final Map<Long, BasStudyRoomEntity> rooms;
		private final Map<Long, BasFloorEntity> floors;

		AppointmentPlaceLookup(Map<Long, BasSeatEntity> seats, Map<Long, BasStudyRoomEntity> rooms,
				Map<Long, BasFloorEntity> floors) {
			this.seats = seats;
			this.rooms = rooms;
			this.floors = floors;
		}

		static AppointmentPlaceLookup empty() {
			return new AppointmentPlaceLookup(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
		}

		BasSeatEntity seat(Long seatId) {
			return seatId == null ? null : seats.get(seatId);
		}

		BasStudyRoomEntity room(Long roomId) {
			return roomId == null ? null : rooms.get(roomId);
		}

		BasFloorEntity floor(Long floorId) {
			return floorId == null ? null : floors.get(floorId);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean adminCancel(Long id) {
		BasAppointmentEntity r = getById(id);
		if (r == null) {
			return false;
		}
		Integer st = r.getSeatState();
		if (st == null || st != 0) {
			return false;
		}
		if (r.getBizDate() == null || r.getSlotStart() == null || r.getSlotEnd() == null) {
			return false;
		}
		pendingCheckIn.cancelSchedule(id);
		appointmentEnd.cancelSchedule(id);
		orchestrator.release(r.getSeatId(), r.getBizDate(), r.getSlotStart(), r.getSlotEnd());
		r.setSeatState(2);
		return updateById(r);
	}

	/**
	 * 计费小时数：向上取整到整小时。<br>
	 * 例如 61 分钟 → 2 小时，119 分钟 → 2 小时，120 分钟 → 2 小时，121 分钟 → 3 小时。
	 */
	private static int computeBillableHours(int totalMinutes) {
		if (totalMinutes <= 0) return 0;
		return (totalMinutes + 59) / 60;
	}

	/**
	 * 带重试的乐观锁更新：updateById 返回 0（版本冲突）时重新读取最新数据再尝试，最多 3 次
	 * @param id 预约 ID
	 * @param modifier 状态变更操作（在最新实体上执行）
	 * @param failMsg 全部重试失败后的提示
	 */
	private void retryUpdateState(Long id, java.util.function.Consumer<BasAppointmentEntity> modifier, String failMsg) {
		int maxRetries = 3;
		for (int i = 0; i < maxRetries; i++) {
			BasAppointmentEntity r = getById(id);
			if (r == null) throw new RRException("预约不存在");
			modifier.accept(r);
			if (updateById(r)) return;
		}
		throw new RRException(failMsg);
	}
}
