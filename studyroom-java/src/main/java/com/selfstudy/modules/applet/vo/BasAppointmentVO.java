package com.selfstudy.modules.applet.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 我的预约信息返回模型
 */
@Data
public class BasAppointmentVO {
    /**
     * id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 座位id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long seatId;
    /**
     * 预约者id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    /**
     * 预约者电话
     */
    private String seatPhone;
    /**
     * 预约者姓名
     */
    private String seatName;
    /**
     * 预约者专业班级
     */
    private String seatClass;
    /**
     * 审核状态
     */
    private Integer seatState;
    /**
     * 预约时间区间
     */
    private String seatDay;



    /**
     * 座位名称
     */
    @JsonProperty("sname")
    private String sName;
    /**
     * 自习室名称
     */
    private String roomName;
    /**
     * 楼层
     */
    private String floor;

    /** 计价金额 */
    private BigDecimal payAmount;
    /** 0 会员免费 1 按次已付(模拟) */
    private Integer payStatus;

    /** 签到时间（毫秒时间戳，可为空） */
    private Long checkInAt;
    /** 结束学习完成时间（毫秒时间戳，可为空） */
    private Long studyEndAt;
}
