package com.selfstudy.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.modules.sys.dao.SysLogDao;
import com.selfstudy.modules.sys.entity.SysLogEntity;
import com.selfstudy.modules.sys.service.SysLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogDao, SysLogEntity> implements SysLogService {

	private static final Logger log = LoggerFactory.getLogger(SysLogServiceImpl.class);

	@Override
	public boolean save(SysLogEntity entity) {
		try {
			return super.save(entity);
		} catch (Exception e) {
			log.warn("sys_log 表可能未创建，跳过落库: {}", e.getMessage());
			return true;
		}
	}
}
