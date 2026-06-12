package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 座位时段策略：在日期范围内每日某时段，要么全员不可订，要么仅白名单用户可订。
 */
@Data
@TableName("bas_seat_access_rule")
public class BasSeatAccessRuleEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long seatId;
	/** yyyy-MM-dd */
	private String dateFrom;
	private String dateTo;
	/** HH:mm:ss 或 HH:mm */
	private String timeFrom;
	private String timeTo;
	/** 0 全员不可订 1 仅白名单可订 */
	private Integer lockMode;
	private String whitelistUserIds;
	private Integer enabled;
	private String remark;
	private Date createTime;
	private Date updateTime;
}
