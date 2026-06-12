package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bas_seat")
public class BasSeatEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long roomId;
	/** 座位编号/名称 */
	private String seatName;
	private Integer gridRow;
	private Integer gridCol;
	/** 0 单人座 1 双人座 2 包厢 */
	private Integer seatType;
	/** 0 可约 1 锁座 */
	private Integer locked;

	@JsonProperty("seatId")
	public Long getSeatId() {
		return id;
	}
}
