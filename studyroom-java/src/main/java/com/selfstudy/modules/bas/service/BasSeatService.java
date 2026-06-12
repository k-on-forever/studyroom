package com.selfstudy.modules.bas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.modules.bas.entity.BasSeatEntity;

import java.util.List;

public interface BasSeatService extends IService<BasSeatEntity> {
	List<BasSeatEntity> getSeatByRoom(Long roomId);
}
