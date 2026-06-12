package com.selfstudy.modules.bas.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.vo.AppointmentRoomCountVO;
import com.selfstudy.modules.bas.vo.PeakSlotStatVO;
import com.selfstudy.modules.bas.vo.SeatPopularityVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BasAppointmentDao extends BaseMapper<BasAppointmentEntity> {

	@Select("SELECT f.floor_name AS floor, r.room_name AS roomName, COUNT(a.id) AS count "
			+ "FROM bas_appointment a "
			+ "INNER JOIN bas_seat s ON a.seat_id = s.id "
			+ "INNER JOIN bas_study_room r ON s.room_id = r.id "
			+ "INNER JOIN bas_floor f ON r.floor_id = f.id "
			+ "WHERE a.biz_date = #{date} "
			+ "GROUP BY f.id, f.floor_name, r.id, r.room_name "
			+ "ORDER BY f.id, r.id")
	List<AppointmentRoomCountVO> selectCountGroupByRoom(@Param("date") String date);

	@Select("SELECT slot_start AS slotStart, COUNT(*) AS cnt FROM bas_appointment "
			+ "WHERE biz_date >= #{from} AND biz_date <= #{to} AND seat_state IN (0,1,3,4) "
			+ "GROUP BY slot_start ORDER BY slot_start")
	List<PeakSlotStatVO> statPeakSlots(@Param("from") String from, @Param("to") String to);

	@Select("SELECT s.id AS seatId, s.seat_name AS seatName, COUNT(*) AS cnt FROM bas_appointment a "
			+ "INNER JOIN bas_seat s ON a.seat_id = s.id "
			+ "WHERE a.biz_date >= #{from} AND a.biz_date <= #{to} AND IFNULL(a.seat_state,0) <> 2 "
			+ "GROUP BY s.id, s.seat_name ORDER BY cnt DESC LIMIT #{limit}")
	List<SeatPopularityVO> statPopularSeats(@Param("from") String from, @Param("to") String to,
			@Param("limit") int limit);
}
