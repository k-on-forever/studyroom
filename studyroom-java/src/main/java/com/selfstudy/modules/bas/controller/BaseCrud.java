package com.selfstudy.modules.bas.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.common.utils.R;

import java.util.Map;

/**
 * CRUD Controller 通用工具类，消除 info / delete 的重复代码。
 */
public class BaseCrud {

	public static <T> R info(IService<T> service, Long id) {
		T e = service.getById(id);
		if (e == null) {
			return R.error("记录不存在");
		}
		return R.ok().put("data", e);
	}

	public static R delete(IService<?> service, Map<String, Long> body) {
		Long id = body == null ? null : body.get("id");
		if (id == null) {
			return R.error("id 不能为空");
		}
		return service.removeById(id) ? R.ok() : R.error("删除失败");
	}
}
