package com.selfstudy.modules.reservation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.modules.bas.dao.BasAppointmentDao;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 按数据库有效预约重建 Redis 座位位图（运维：Redis 清空或漂移后调用）。
 */
@Service
public class SeatBitmapRebuildService {

	private static final Logger log = LoggerFactory.getLogger(SeatBitmapRebuildService.class);

	private final BasAppointmentDao basAppointmentDao;
	private final SeatBitmapRedisService bitmap;
	private final StringRedisTemplate redis;

	public SeatBitmapRebuildService(BasAppointmentDao basAppointmentDao, SeatBitmapRedisService bitmap,
			StringRedisTemplate redis) {
		this.basAppointmentDao = basAppointmentDao;
		this.bitmap = bitmap;
		this.redis = redis;
	}

	/**
	 * @return 参与重建的有效预约条数
	 */
	public int rebuildFromDatabase() {
		List<BasAppointmentEntity> rows = basAppointmentDao.selectList(
				new LambdaQueryWrapper<BasAppointmentEntity>()
						.in(BasAppointmentEntity::getSeatState, 0, 1)
						.isNotNull(BasAppointmentEntity::getSeatId)
						.isNotNull(BasAppointmentEntity::getBizDate)
						.isNotNull(BasAppointmentEntity::getSlotStart)
						.isNotNull(BasAppointmentEntity::getSlotEnd));
		Set<String> cleared = new HashSet<>();
		for (BasAppointmentEntity r : rows) {
			Long seatId = r.getSeatId();
			String bizDate = r.getBizDate().trim();
			String key = bitmap.bitmapKey(seatId, bizDate);
			if (cleared.add(key)) {
				redis.delete(key);
			}
		}
		for (BasAppointmentEntity r : rows) {
			int start = r.getSlotStart();
			int end = r.getSlotEnd();
			if (end > start) {
				bitmap.tryOccupyRange(r.getSeatId(), r.getBizDate().trim(), start, end);
			}
		}
		log.info("rebuild seat bitmaps from DB: appointments={}, bitmapKeys={}", rows.size(), cleared.size());
		return rows.size();
	}
}
