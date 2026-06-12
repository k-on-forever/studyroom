package com.selfstudy.modules.applet.dto;

import lombok.Data;

@Data
public class StudyDayCellDTO {
	private String date;
	private int minutes;
	/** 0–4，用于热力深浅 */
	private int level;
}
