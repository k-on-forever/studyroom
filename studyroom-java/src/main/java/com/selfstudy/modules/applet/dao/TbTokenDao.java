package com.selfstudy.modules.applet.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfstudy.modules.applet.entity.TbTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 小程序用户Token
 * 
 * @author kon-forever
 * @email 2891517520@qq.com
 * @date 2023-01-31 14:21:07
 */
@Mapper
public interface TbTokenDao extends BaseMapper<TbTokenEntity> {

	@Select("select * from tb_token where token = #{token} limit 1")
	TbTokenEntity queryByToken(@Param("token") String token);
}
