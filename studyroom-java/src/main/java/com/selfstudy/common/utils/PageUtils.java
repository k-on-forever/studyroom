package com.selfstudy.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.List;

/**
 * 分页工具（人人风格）。
 */
public class PageUtils implements Serializable {
	private static final long serialVersionUID = 1L;
	private int totalCount;
	private int pageSize;
	private int totalPage;
	private int currPage;
	private List<?> list;

	public PageUtils(List<?> list, int totalCount, int pageSize, int currPage) {
		this.list = list;
		this.totalCount = totalCount;
		this.pageSize = pageSize;
		this.currPage = currPage;
		this.totalPage = (int) Math.ceil((double) totalCount / pageSize);
	}

	public PageUtils(IPage<?> page) {
		this.list = page.getRecords();
		this.totalCount = (int) page.getTotal();
		this.pageSize = (int) page.getSize();
		this.currPage = (int) page.getCurrent();
		this.totalPage = (int) page.getPages();
	}

	public int getTotalCount() {
		return totalCount;
	}

	public int getPageSize() {
		return pageSize;
	}

	public int getTotalPage() {
		return totalPage;
	}

	public int getCurrPage() {
		return currPage;
	}

	public List<?> getList() {
		return list;
	}
}
