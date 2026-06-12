package com.selfstudy.modules.bas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.common.base.PageResult;
import com.selfstudy.common.base.QueryInfoDTO;
import com.selfstudy.modules.bas.dao.BasNoticeDao;
import com.selfstudy.modules.bas.entity.BasNoticeEntity;
import com.selfstudy.modules.bas.service.BasNoticeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class BasNoticeServiceImpl extends ServiceImpl<BasNoticeDao, BasNoticeEntity> implements BasNoticeService {

	@Override
	public PageResult<BasNoticeEntity> listNotice(QueryInfoDTO query) {
		long page = query.getPage() == null ? 1L : query.getPage();
		long limit = query.getLimit() == null ? 10L : query.getLimit();
		LambdaQueryWrapper<BasNoticeEntity> w = new LambdaQueryWrapper<>();
		if (StringUtils.isNotBlank(query.getKeyword())) {
			w.and(q -> q.like(BasNoticeEntity::getTitle, query.getKeyword())
					.or()
					.like(BasNoticeEntity::getContent, query.getKeyword()));
		}
		w.orderByDesc(BasNoticeEntity::getCreateTime);
		Page<BasNoticeEntity> p = page(new Page<>(page, limit), w);
		return new PageResult<>(p.getRecords(), p.getTotal());
	}
}
