package com.selfstudy.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfstudy.modules.sys.entity.SysLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLogDao extends BaseMapper<SysLogEntity> {
}
