package com.selfstudy.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfstudy.modules.sys.entity.SysAdminTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysAdminTokenDao extends BaseMapper<SysAdminTokenEntity> {

	@Select("SELECT * FROM sys_admin_token WHERE token = #{token} LIMIT 1")
	SysAdminTokenEntity selectByToken(@Param("token") String token);
}
