package com.selfstudy.modules.applet.dto.save;

import lombok.Data;

@Data
public class BasAppointmentSaveDTO {
    /**
     * 座位id
     */
    private Long seatId;
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
     * 预约时间区间
     */
    private String seatDay;

    /**
     * 演示环境（study.wx.pay.mode=mock）非会员模拟已付
     */
    private Boolean simulatePaidNonMember;

    /**
     * 演示用：模拟支付渠道 WECHAT / ALIPAY
     */
    private String simulatedPayChannel;

    /** 微信支付单号（study.wx.pay.mode=wx 时必填，须先 prepay 再 requestPayment） */
    private String wxPayOutTradeNo;
}
