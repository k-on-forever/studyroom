package com.selfstudy.modules.bas.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class BasAppointmentAdminVO {
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long seatId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long userId;
	private String seatPhone;
	private String seatName;
	private String seatClass;
	private Integer seatState;
	private String seatDay;
	private String bizDate;
	private Integer slotStart;
	private Integer slotEnd;
	private Date createTime;
	private Date checkInAt;
	private Date studyEndAt;
	private String floor;
	private String roomName;
	private String seatLabel;
}
