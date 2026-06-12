package com.selfstudy.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.modules.sys.dao.SysMenuDao;
import com.selfstudy.modules.sys.entity.SysMenuEntity;
import com.selfstudy.modules.sys.service.SysNavService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysNavServiceImpl implements SysNavService {

	private final SysMenuDao sysMenuDao;

	@Override
	public Map<String, Object> buildNavPayload(Long adminId) {
		List<Long> roleMenuIds = sysMenuDao.listMenuIdsByAdminId(adminId);
		if (roleMenuIds == null || roleMenuIds.isEmpty()) {
			Map<String, Object> empty = new HashMap<>(2);
			empty.put("menuList", List.of());
			empty.put("permissions", List.of());
			return empty;
		}

		Set<Long> allIds = new LinkedHashSet<>(roleMenuIds);
		boolean growing = true;
		while (growing) {
			growing = false;
			List<SysMenuEntity> batch = sysMenuDao.selectList(
					new LambdaQueryWrapper<SysMenuEntity>().in(SysMenuEntity::getMenuId, allIds));
			for (SysMenuEntity m : batch) {
				Long p = m.getParentId();
				if (p != null && p > 0 && allIds.add(p)) {
					growing = true;
				}
			}
		}

		List<SysMenuEntity> rows = sysMenuDao.selectList(
				new LambdaQueryWrapper<SysMenuEntity>()
						.in(SysMenuEntity::getMenuId, allIds)
						.orderByAsc(SysMenuEntity::getOrderNum));

		Map<Long, List<SysMenuEntity>> byParent = rows.stream()
				.collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId(), LinkedHashMap::new,
						Collectors.toCollection(ArrayList::new)));
		for (List<SysMenuEntity> list : byParent.values()) {
			list.sort(Comparator.comparingInt(m -> m.getOrderNum() == null ? 0 : m.getOrderNum()));
		}

		List<String> permissions = rows.stream()
				.map(SysMenuEntity::getPerms)
				.filter(StringUtils::hasText)
				.distinct()
				.collect(Collectors.toList());

		Map<String, Object> out = new HashMap<>(2);
		out.put("menuList", buildLevel(byParent, 0L));
		out.put("permissions", permissions);
		return out;
	}

	private List<Map<String, Object>> buildLevel(Map<Long, List<SysMenuEntity>> byParent, long parentId) {
		List<SysMenuEntity> level = byParent.getOrDefault(parentId, List.of());
		List<Map<String, Object>> list = new ArrayList<>(level.size());
		for (SysMenuEntity m : level) {
			Map<String, Object> node = new LinkedHashMap<>();
			node.put("menuId", m.getMenuId());
			node.put("parentId", m.getParentId());
			node.put("name", m.getName());
			node.put("url", m.getUrl());
			node.put("type", m.getType());
			node.put("icon", m.getIcon());
			List<Map<String, Object>> sub = buildLevel(byParent, m.getMenuId());
			if (!sub.isEmpty()) {
				node.put("list", sub);
			}
			list.add(node);
		}
		return list;
	}
}
