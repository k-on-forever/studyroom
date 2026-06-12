package com.selfstudy.modules.sys.service;

/**
 * 管理端权限：基于 sys_menu.perms 与角色菜单关联。
 */
public interface AdminPermissionService {

	boolean hasPermission(Long adminId, String perm);
}
