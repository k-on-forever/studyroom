package com.selfstudy.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.modules.sys.dao.SysAdminTokenDao;
import com.selfstudy.modules.sys.entity.SysAdminTokenEntity;
import com.selfstudy.modules.sys.service.SysAdminTokenService;
import org.springframework.stereotype.Service;

@Service
public class SysAdminTokenServiceImpl extends ServiceImpl<SysAdminTokenDao, SysAdminTokenEntity>
		implements SysAdminTokenService {

	@Override
	public SysAdminTokenEntity queryByToken(String token) {
		return baseMapper.selectByToken(token);
	}
}
