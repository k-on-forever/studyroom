package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("bas_wx_pay_order")
public class BasWxPayOrderEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long userId;
	/** APPOINTMENT / MEMBERSHIP */
	private String bizType;
	private String bizRef;
	private String outTradeNo;
	private Integer amountFen;
	/** 0 待支付 1 已支付 2 已关闭 */
	private Integer status;
	private String payloadJson;
	private String wxTransactionId;
	private Date paidTime;
	private Date createTime;
}
