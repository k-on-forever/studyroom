package com.selfstudy.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfstudy.modules.sys.entity.SysMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuDao extends BaseMapper<SysMenuEntity> {

	@Select("SELECT DISTINCT rm.menu_id FROM sys_admin_role ar INNER JOIN sys_role_menu rm ON ar.role_id = rm.role_id WHERE ar.admin_id = #{adminId}")
	List<Long> listMenuIdsByAdminId(@Param("adminId") Long adminId);

	@Select("SELECT DISTINCT m.perms FROM sys_menu m INNER JOIN sys_role_menu rm ON m.menu_id = rm.menu_id "
			+ "INNER JOIN sys_admin_role ar ON rm.role_id = ar.role_id WHERE ar.admin_id = #{adminId} "
			+ "AND m.perms IS NOT NULL AND CHAR_LENGTH(TRIM(m.perms)) > 0")
	List<String> listPermsByAdminId(@Param("adminId") Long adminId);

	@Select("SELECT COUNT(*) FROM sys_admin_role WHERE admin_id = #{adminId} AND role_id = 1")
	int countSuperAdminRole(@Param("adminId") Long adminId);
}
