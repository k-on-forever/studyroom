package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bas_study_room")
public class BasStudyRoomEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long floorId;
	private String roomName;
	/** 位置/区域描述 */
	private String roomLocation;
	/** 展示用营业时间（可为空） */
	private String openingTime;
	private String closeTime;
	/** 与批量排座一致的行数/列数（0 表示未在库内排座或仅手工座位） */
	private Integer seatRows;
	private Integer seatCols;
	/**
	 * 时段粒度（分钟），须为 10、30 或 60，且须与全局配置 {@code study.reservation.slot-minutes} 一致。
	 */
	private Integer slotStepMinutes;

	@JsonProperty("roomId")
	public Long getRoomId() {
		return id;
	}
}
