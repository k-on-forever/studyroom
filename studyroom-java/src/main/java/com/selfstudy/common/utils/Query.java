package com.selfstudy.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;

/**
 * 从请求参数 Map 构造 MyBatis-Plus 分页。
 */
public class Query<T> {

	public IPage<T> getPage(Map<String, Object> params) {
		long curPage = 1;
		long limit = 10;
		if (params != null) {
			if (params.get("page") != null) {
				curPage = Long.parseLong(params.get("page").toString());
			}
			if (params.get("limit") != null) {
				limit = Long.parseLong(params.get("limit").toString());
			}
		}
		return new Page<>(curPage, limit);
	}
}
