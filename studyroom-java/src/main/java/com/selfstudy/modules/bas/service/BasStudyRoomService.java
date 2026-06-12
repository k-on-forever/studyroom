package com.selfstudy.modules.bas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;

import java.util.List;

public interface BasStudyRoomService extends IService<BasStudyRoomEntity> {
	List<BasStudyRoomEntity> getRoomByFloor(Long floorId);
}
