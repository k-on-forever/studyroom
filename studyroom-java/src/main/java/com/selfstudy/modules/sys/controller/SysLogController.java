package com.selfstudy.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.sys.entity.SysLogEntity;
import com.selfstudy.modules.sys.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 管理后台 —— 操作日志查询（数据由 SysLogAspect 自动写入 sys_log 表）。
 */
@RestController
@RequestMapping("/sys/log")
@RequiredArgsConstructor
@Tag(name = "管理后台-操作日志")
public class SysLogController {

    private final SysLogService sysLogService;

    @GetMapping("/list")
    @Operation(summary = "操作日志分页列表")
    public R list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "operation", required = false) String operation,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        LambdaQueryWrapper<SysLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            wrapper.like(SysLogEntity::getUsername, username.trim());
        }
        if (StringUtils.isNotBlank(operation)) {
            wrapper.like(SysLogEntity::getOperation, operation.trim());
        }
        if (StringUtils.isNotBlank(startDate)) {
            wrapper.ge(SysLogEntity::getCreateDate, startDate + " 00:00:00");
        }
        if (StringUtils.isNotBlank(endDate)) {
            wrapper.le(SysLogEntity::getCreateDate, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(SysLogEntity::getCreateDate);

        Page<SysLogEntity> pageResult = sysLogService.page(
                new Page<>(page, limit), wrapper);

        return R.ok()
                .put("page", pageResult.getCurrent())
                .put("limit", pageResult.getSize())
                .put("total", pageResult.getTotal())
                .put("list", pageResult.getRecords());
    }

    @GetMapping("/recent")
    @Operation(summary = "最近N条操作日志（首页仪表盘用）")
    public R recent(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        LambdaQueryWrapper<SysLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysLogEntity::getCreateDate).last("LIMIT " + limit);
        List<SysLogEntity> list = sysLogService.list(wrapper);
        return R.ok().put("data", list);
    }
}
