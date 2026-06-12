package com.selfstudy.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.modules.sys.entity.SysAdminTokenEntity;

public interface SysAdminTokenService extends IService<SysAdminTokenEntity> {

	SysAdminTokenEntity queryByToken(String token);
}
