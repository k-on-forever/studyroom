package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("bas_user_hour_wallet")
public class BasUserHourWalletEntity {
	@TableId(value = "user_id")
	private Long userId;
	private BigDecimal balanceHours;
	private Date updateTime;
}
