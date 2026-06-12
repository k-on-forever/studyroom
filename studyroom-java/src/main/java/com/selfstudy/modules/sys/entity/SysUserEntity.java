package com.selfstudy.modules.sys.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class SysUserEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long userId;
	private String username;
}
