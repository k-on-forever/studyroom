package com.selfstudy.common.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一 JSON 返回（人人风格）。
 */
public class R extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	public static R error() {
		return error(500, "未知异常，请联系管理员");
	}

	public static R error(String msg) {
		return error(500, msg);
	}

	public static R error(int code, String msg) {
		R r = new R();
		r.put("code", code);
		r.put("msg", msg);
		return r;
	}

	public static R ok(String msg) {
		R r = new R();
		r.put("msg", msg);
		r.put("code", 0);
		return r;
	}

	public static R ok(Map<String, Object> map) {
		R r = new R();
		r.put("code", 0);
		r.putAll(map);
		return r;
	}

	public static R ok() {
		R r = new R();
		r.put("code", 0);
		r.put("msg", "success");
		return r;
	}

	@Override
	public R put(String key, Object value) {
		super.put(key, value);
		return this;
	}
}
