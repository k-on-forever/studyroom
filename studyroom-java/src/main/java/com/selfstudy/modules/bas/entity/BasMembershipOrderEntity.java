package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("bas_membership_order")
public class BasMembershipOrderEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long userId;
	private Long cardId;
	private Long couponId;
	private String orderNo;
	private String cardName;
	private BigDecimal amountYuan;
	private BigDecimal discountYuan;
	/** 0 待支付 1 已支付 */
	private Integer payStatus;
	private Date validFrom;
	private Date validTo;
	/** 激活日期修改次数（每张卡最多 3 次，不含首次激活） */
	private Integer activateChangeCount;
	private Date createTime;
	private Date payTime;
}
