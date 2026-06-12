package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bas_floor")
public class BasFloorEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId(type = IdType.AUTO)
	private Long id;
	@TableField("floor_name")
	private String floorName;

	/** 与小程序原字段名 floor 对齐 */
	@JsonProperty("floor")
	public String getFloor() {
		return floorName;
	}
}
