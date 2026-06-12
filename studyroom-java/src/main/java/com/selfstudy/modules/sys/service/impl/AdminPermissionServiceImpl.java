package com.selfstudy.modules.sys.service.impl;

import com.selfstudy.modules.sys.dao.SysMenuDao;
import com.selfstudy.modules.sys.service.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminPermissionServiceImpl implements AdminPermissionService {

	private final SysMenuDao sysMenuDao;

	@Override
	public boolean hasPermission(Long adminId, String perm) {
		if (adminId == null || perm == null || perm.isEmpty()) {
			return false;
		}
		if (sysMenuDao.countSuperAdminRole(adminId) > 0) {
			return true;
		}
		List<String> dbPerms = sysMenuDao.listPermsByAdminId(adminId);
		if (dbPerms == null || dbPerms.isEmpty()) {
			return false;
		}
		Set<String> set = new HashSet<>();
		for (String p : dbPerms) {
			if (p != null && !p.isBlank()) {
				set.add(p.trim());
			}
		}
		return set.contains(perm.trim());
	}
}
