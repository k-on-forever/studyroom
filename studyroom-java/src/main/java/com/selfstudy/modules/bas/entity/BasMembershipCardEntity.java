package com.selfstudy.modules.bas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("bas_membership_card")
public class BasMembershipCardEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId(type = IdType.AUTO)
	private Long id;
	/** MONTH / QUARTER / YEAR / OTHER */
	private String cardKind;
	/** 0 期限畅约 1 学时包 */
	private Integer benefitMode;
	private String cardName;
	private BigDecimal priceYuan;
	private Integer validityDays;
	/** 学时包：包含小时数 */
	private BigDecimal includedHours;
	private String benefitDesc;
	/** 1 上架 0 下架 */
	private Integer onShelf;
	private Integer sortOrder;
	private Date createTime;
	private Date updateTime;
}
