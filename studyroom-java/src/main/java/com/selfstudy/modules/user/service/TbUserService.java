package com.selfstudy.modules.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.modules.user.entity.TbUserEntity;

import java.util.List;

public interface TbUserService extends IService<TbUserEntity> {
	List<TbUserEntity> listForBan();
}
