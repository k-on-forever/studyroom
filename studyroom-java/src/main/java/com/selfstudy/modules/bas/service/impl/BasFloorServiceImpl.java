package com.selfstudy.modules.bas.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.modules.bas.dao.BasFloorDao;
import com.selfstudy.modules.bas.entity.BasFloorEntity;
import com.selfstudy.modules.bas.service.BasFloorService;
import org.springframework.stereotype.Service;

@Service
public class BasFloorServiceImpl extends ServiceImpl<BasFloorDao, BasFloorEntity> implements BasFloorService {
}
