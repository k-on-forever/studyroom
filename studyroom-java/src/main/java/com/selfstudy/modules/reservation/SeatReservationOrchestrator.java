package com.selfstudy.modules.reservation;

import org.springframework.stereotype.Service;

/**
 * 座位预约编排：分布式锁 + Bitmap 原子占用。
 */
@Service
public class SeatReservationOrchestrator {

	private final ReservationDistributedLock lock;
	private final SeatBitmapRedisService bitmap;

	public SeatReservationOrchestrator(ReservationDistributedLock lock, SeatBitmapRedisService bitmap) {
		this.lock = lock;
		this.bitmap = bitmap;
	}

	public boolean tryReserve(Long seatId, String bizDate, int slotStartInclusive, int slotEndExclusive) throws Exception {
		return lock.execute(seatId, bizDate, () ->
				bitmap.tryOccupyRange(seatId, bizDate, slotStartInclusive, slotEndExclusive));
	}

	public void release(Long seatId, String bizDate, int slotStartInclusive, int slotEndExclusive) {
		bitmap.releaseRange(seatId, bizDate, slotStartInclusive, slotEndExclusive);
	}
}
