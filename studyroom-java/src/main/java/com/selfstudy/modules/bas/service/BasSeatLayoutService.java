package com.selfstudy.modules.bas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.Data;

import java.util.List;
import java.util.Objects;

/**
 * 按行列批量重排某自习室座位；若该室任一座位已有预约则拒绝。
 */
@Service
@RequiredArgsConstructor
public class BasSeatLayoutService {

	private final BasStudyRoomService basStudyRoomService;
	private final BasSeatService basSeatService;
	private final BasAppointmentService basAppointmentService;

	@Data
	public static class CanvasSeatCell {
		private Long id;
		private Integer r;
		private Integer c;
		private String seatName;
		private Integer seatType;
		private Boolean deleted;
	}

	/** 可视化编辑器：增删改坐标与类型（删除已有预约的座位会拒绝） */
	@Transactional(rollbackFor = Exception.class)
	public void saveCanvasLayout(long roomId, List<CanvasSeatCell> cells) {
		if (roomId < 1) {
			throw new IllegalArgumentException("自习室不合法");
		}
		BasStudyRoomEntity room = basStudyRoomService.getById(roomId);
		if (room == null) {
			throw new IllegalStateException("自习室不存在");
		}
		if (cells == null) {
			return;
		}
		for (CanvasSeatCell cell : cells) {
			if (cell == null) {
				continue;
			}
			if (Boolean.TRUE.equals(cell.getDeleted()) && cell.getId() != null) {
				long n = basAppointmentService.count(
						new LambdaQueryWrapper<BasAppointmentEntity>()
								.eq(BasAppointmentEntity::getSeatId, cell.getId()));
				if (n > 0) {
					throw new IllegalStateException("座位 id=" + cell.getId() + " 已有预约，无法删除");
				}
				BasSeatEntity ex = basSeatService.getById(cell.getId());
				if (ex != null && Objects.equals(ex.getRoomId(), roomId)) {
					basSeatService.removeById(cell.getId());
				}
				continue;
			}
			int r = cell.getR() == null ? 0 : cell.getR();
			int c = cell.getC() == null ? 0 : cell.getC();
			int st = cell.getSeatType() == null ? 0 : cell.getSeatType();
			if (r < 1 || c < 1) {
				throw new IllegalArgumentException("行列须从 1 开始");
			}
			if (cell.getId() != null) {
				BasSeatEntity e = basSeatService.getById(cell.getId());
				if (e == null || !Objects.equals(e.getRoomId(), roomId)) {
					throw new IllegalStateException("座位不存在或不属于本自习室");
				}
				e.setGridRow(r);
				e.setGridCol(c);
				e.setSeatType(st);
				if (StringUtils.hasText(cell.getSeatName())) {
					e.setSeatName(cell.getSeatName().trim());
				}
				basSeatService.updateById(e);
			} else {
				BasSeatEntity e = new BasSeatEntity();
				e.setRoomId(roomId);
				e.setGridRow(r);
				e.setGridCol(c);
				e.setSeatType(st);
				e.setLocked(0);
				String nm = StringUtils.hasText(cell.getSeatName()) ? cell.getSeatName().trim() : ("S-" + r + "-" + c);
				e.setSeatName(nm);
				basSeatService.save(e);
			}
		}
		int maxR = 0;
		int maxC = 0;
		List<BasSeatEntity> all = basSeatService.getSeatByRoom(roomId);
		for (BasSeatEntity s : all) {
			if (s.getGridRow() != null && s.getGridRow() > maxR) {
				maxR = s.getGridRow();
			}
			if (s.getGridCol() != null && s.getGridCol() > maxC) {
				maxC = s.getGridCol();
			}
		}
		room.setSeatRows(maxR);
		room.setSeatCols(maxC);
		basStudyRoomService.updateById(room);
	}

	@Transactional(rollbackFor = Exception.class)
	public void batchRegenerateSeats(long roomId, int rows, int cols, String namePrefix) {
		if (roomId < 1 || rows < 1 || cols < 1) {
			throw new IllegalArgumentException("房间与行列数不合法");
		}
		BasStudyRoomEntity room = basStudyRoomService.getById(roomId);
		if (room == null) {
			throw new IllegalStateException("自习室不存在");
		}
		List<BasSeatEntity> existing = basSeatService.getSeatByRoom(roomId);
		if (!existing.isEmpty()) {
			for (BasSeatEntity s : existing) {
				long c = basAppointmentService.count(
						new LambdaQueryWrapper<BasAppointmentEntity>()
								.eq(BasAppointmentEntity::getSeatId, s.getId()));
				if (c > 0) {
					throw new IllegalStateException("本自习室已有带预约的座位，无法重排。请先移走预约或单座维护。");
				}
			}
			basSeatService.remove(new LambdaQueryWrapper<BasSeatEntity>()
					.eq(BasSeatEntity::getRoomId, roomId));
		}
		String p = StringUtils.hasText(namePrefix) ? namePrefix.trim() : "S";
		for (int r = 1; r <= rows; r++) {
			for (int c = 1; c <= cols; c++) {
				BasSeatEntity e = new BasSeatEntity();
				e.setRoomId(roomId);
				e.setSeatName(p + "-" + r + "-" + c);
				e.setGridRow(r);
				e.setGridCol(c);
				e.setLocked(0);
				basSeatService.save(e);
			}
		}
		room.setSeatRows(rows);
		room.setSeatCols(cols);
		basStudyRoomService.updateById(room);
	}
}
