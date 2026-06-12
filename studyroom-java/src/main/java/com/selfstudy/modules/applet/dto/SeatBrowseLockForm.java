package com.selfstudy.modules.applet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeatBrowseLockForm {

	@NotNull(message = "座位不能为空")
	@Schema(description = "座位 id")
	private Long seatId;

	@NotNull(message = "时段不能为空")
	@Schema(description = "业务日时段 yyyy-MM-dd/HH:mm-HH:mm")
	private String seatDay;
}
