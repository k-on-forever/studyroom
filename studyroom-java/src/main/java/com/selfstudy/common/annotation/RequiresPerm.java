package com.selfstudy.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端接口权限（与 {@code sys_menu.perms} 一致）。未标注的方法仅需登录。
 * 拥有角色 {@code role_id = 1} 的超级管理员跳过校验。
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPerm {
	/** 权限标识，如 bas:appointment:list */
	String value();
}
