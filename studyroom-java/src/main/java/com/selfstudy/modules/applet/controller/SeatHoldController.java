package com.selfstudy.modules.applet.controller;

import com.selfstudy.modules.applet.annotation.Login;
import com.selfstudy.modules.reservation.StudySeatLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 座位 Redis 占位调试接口；正式预约已在 {@code BasAppointmentServiceImpl} 内调用 {@code StudySeatLockService}。
 */
@RestController
@RequestMapping("/applet/seat-lock")
@Tag(name = "座位Redis占位(调试)")
@ConditionalOnBean(StudySeatLockService.class)
public class SeatHoldController {

    @Autowired
    private StudySeatLockService studySeatLockService;

    @Login
    @PostMapping("/hold")
    @Operation(summary = "尝试占位（SET NX + TTL）")
    public Map<String, Object> hold(@RequestBody HoldSeatRequest body, @RequestAttribute("userId") Long userId) {
        boolean ok = studySeatLockService.tryHold(body.getSeatId(), body.getSeatDay(), userId);
        Map<String, Object> m = new HashMap<>(4);
        m.put("success", ok);
        m.put("message", ok ? "占位成功" : "该时段座位已被他人锁定，请稍后或换座");
        return m;
    }

    @Login
    @PostMapping("/release")
    @Operation(summary = "释放本人占位")
    public Map<String, Object> release(@RequestBody HoldSeatRequest body, @RequestAttribute("userId") Long userId) {
        studySeatLockService.releaseHold(body.getSeatId(), body.getSeatDay(), userId);
        Map<String, Object> m = new HashMap<>(2);
        m.put("success", true);
        return m;
    }

    public static class HoldSeatRequest {
        private Long seatId;
        /** 与预约接口里 seatDay 含义一致，例如某日期或时段标识 */
        private String seatDay;

        public Long getSeatId() {
            return seatId;
        }

        public void setSeatId(Long seatId) {
            this.seatId = seatId;
        }

        public String getSeatDay() {
            return seatDay;
        }

        public void setSeatDay(String seatDay) {
            this.seatDay = seatDay;
        }
    }
}
