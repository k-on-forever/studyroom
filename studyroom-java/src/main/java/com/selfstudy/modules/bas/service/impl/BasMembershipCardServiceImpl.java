package com.selfstudy.modules.bas.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.modules.bas.dao.BasMembershipCardDao;
import com.selfstudy.modules.bas.entity.BasMembershipCardEntity;
import com.selfstudy.modules.bas.service.BasMembershipCardService;
import org.springframework.stereotype.Service;

@Service
public class BasMembershipCardServiceImpl extends ServiceImpl<BasMembershipCardDao, BasMembershipCardEntity>
		implements BasMembershipCardService {
}
