package com.selfstudy.modules.bas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.common.base.PageResult;
import com.selfstudy.common.base.QueryInfoDTO;
import com.selfstudy.modules.bas.entity.BasNoticeEntity;

public interface BasNoticeService extends IService<BasNoticeEntity> {
	PageResult<BasNoticeEntity> listNotice(QueryInfoDTO query);
}
