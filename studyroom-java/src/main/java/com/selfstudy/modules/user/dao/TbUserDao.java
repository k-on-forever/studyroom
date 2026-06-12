package com.selfstudy.modules.user.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfstudy.modules.user.entity.TbUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TbUserDao extends BaseMapper<TbUserEntity> {
}
