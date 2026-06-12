package com.selfstudy.modules.bas.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfstudy.modules.bas.entity.BasMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BasMessageDao extends BaseMapper<BasMessageEntity> {
}
