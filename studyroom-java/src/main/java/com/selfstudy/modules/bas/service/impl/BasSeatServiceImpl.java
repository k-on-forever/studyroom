package com.selfstudy.modules.bas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.modules.bas.dao.BasSeatDao;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.service.BasSeatService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasSeatServiceImpl extends ServiceImpl<BasSeatDao, BasSeatEntity> implements BasSeatService {

	@Override
	public List<BasSeatEntity> getSeatByRoom(Long roomId) {
		return list(new LambdaQueryWrapper<BasSeatEntity>()
				.eq(BasSeatEntity::getRoomId, roomId));
	}
}
