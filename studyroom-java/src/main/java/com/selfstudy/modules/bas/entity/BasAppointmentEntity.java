package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("bas_appointment")
public class BasAppointmentEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long seatId;
	private Long userId;
	private String seatPhone;
	private String seatName;
	private String seatClass;
	/** 前端展示用原始区间字符串 */
	private String seatDay;
	/** yyyy-MM-dd */
	private String bizDate;
	/** 相对当日营业起点的槽位下标 [start, end) */
	private Integer slotStart;
	private Integer slotEnd;
	/**
	 * 0 待签到
	 * 1 已签到使用中
	 * 2 已取消
	 * 3 已完成
	 * 4 爽约
	 */
	private Integer seatState;
	/** 计价金额（非会员模拟支付） */
	private BigDecimal payAmount;
	/** 0 会员免费 1 按次已付(模拟) */
	private Integer payStatus;
	private Date createTime;

	/** 乐观锁，防并发更新同一预约记录 */
	@Version
	private Integer version;

	private Date checkInAt;
	private Date studyEndAt;
}
