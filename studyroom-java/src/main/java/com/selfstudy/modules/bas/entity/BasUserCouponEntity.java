package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("bas_user_coupon")
public class BasUserCouponEntity implements Serializable {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long userId;
	private String title;
	private BigDecimal balanceYuan;
	/** 0 有效 1 用尽 2 作废 */
	private Integer status;
	private Date expireTime;
	private Date createTime;
}
