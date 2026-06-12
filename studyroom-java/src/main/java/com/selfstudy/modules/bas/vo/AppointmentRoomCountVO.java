package com.selfstudy.modules.bas.vo;

import lombok.Data;

/**
 * 首页「当日各自习室预约次数」一行
 */
@Data
public class AppointmentRoomCountVO {
	private String floor;
	private String roomName;
	private Long count;
}
