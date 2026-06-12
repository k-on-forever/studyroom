package com.selfstudy.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("tb_user")
public class TbUserEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId(type = IdType.ASSIGN_ID)
	private Long userId;
	private String username;
	private String account;
	private String mobile;
	private String name;
	private Long qq;
	private String email;
	private String bz;
	private Date createTime;
	private String userImg;
	private String openId;
	/** 0 封禁 1 正常 */
	private Integer status;
}
