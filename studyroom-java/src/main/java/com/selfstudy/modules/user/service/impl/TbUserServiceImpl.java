package com.selfstudy.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.modules.user.dao.TbUserDao;
import com.selfstudy.modules.user.entity.TbUserEntity;
import com.selfstudy.modules.user.service.TbUserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TbUserServiceImpl extends ServiceImpl<TbUserDao, TbUserEntity> implements TbUserService {

	@Override
	public List<TbUserEntity> listForBan() {
		return list(new LambdaQueryWrapper<TbUserEntity>().eq(TbUserEntity::getStatus, 0));
	}
}
