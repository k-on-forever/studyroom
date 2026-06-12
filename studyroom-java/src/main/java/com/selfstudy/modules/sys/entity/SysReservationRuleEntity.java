package com.selfstudy.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_reservation_rule")
public class SysReservationRuleEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.INPUT)
	private Integer id;
	private Integer advanceBookingDays;
	private Integer maxDurationMinutes;
	private Integer cancelAdvanceMinutes;
	private Date updateTime;
}
