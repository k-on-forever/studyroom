package com.selfstudy.modules.bas.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface AdminStatsDao {

	@Select("SELECT COUNT(*) FROM bas_seat WHERE IFNULL(locked,0) = 0")
	Long countBookableSeats();

	@Select("SELECT COUNT(DISTINCT seat_id) FROM bas_appointment WHERE biz_date = #{d} AND IFNULL(seat_state,0) <> 2")
	Long countDistinctBookedSeatsOn(@Param("d") String d);

	@Select("SELECT COUNT(*) FROM bas_appointment WHERE seat_state = 1")
	Long countUsingSeatsNow();

	@Select("SELECT COALESCE(SUM(amount_yuan),0) FROM bas_membership_order WHERE pay_status = 1 AND DATE(pay_time) = #{d}")
	BigDecimal sumMembershipRevenueOn(@Param("d") String d);

	@Select("SELECT COUNT(*) FROM bas_membership_order WHERE pay_status = 1 AND DATE(pay_time) = #{d}")
	Long countMembershipOrdersOn(@Param("d") String d);

	@Select("SELECT COALESCE(SUM(pay_amount),0) FROM bas_appointment WHERE pay_status = 1 AND seat_state <> 2 AND DATE(create_time) = #{d}")
	BigDecimal sumAppointmentPayOn(@Param("d") String d);

	@Select("SELECT COUNT(*) FROM bas_appointment WHERE pay_status = 1 AND seat_state <> 2 AND DATE(create_time) = #{d}")
	Long countAppointmentPaysOn(@Param("d") String d);
}
