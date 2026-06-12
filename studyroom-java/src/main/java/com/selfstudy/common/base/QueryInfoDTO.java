package com.selfstudy.common.base;

import lombok.Data;

/**
 * 小程序分页查询参数。
 */
@Data
public class QueryInfoDTO {
	private Long page = 1L;
	private Long limit = 10L;
	private String keyword;
}
