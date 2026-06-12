package com.selfstudy.modules.bas.dao;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfstudy.modules.bas.entity.BasMembershipOrderEntity;

@Mapper
public interface BasMembershipOrderDao extends BaseMapper<BasMembershipOrderEntity> {
}
