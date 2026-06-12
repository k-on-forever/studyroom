package com.selfstudy.modules.bas.vo;

import lombok.Data;

@Data
public class PeakSlotStatVO {
	private Integer slotStart;
	private Long cnt;
	/** 可读时段，如 14:00–15:00（由后端按 study.reservation 解析槽位下标） */
	private String timeRange;
}
