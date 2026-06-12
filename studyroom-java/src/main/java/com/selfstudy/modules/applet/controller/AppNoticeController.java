package com.selfstudy.modules.applet.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.common.base.PageResult;
import com.selfstudy.common.base.QueryInfoDTO;
import com.selfstudy.common.utils.R;
import com.selfstudy.common.validator.ValidatorUtils;
import com.selfstudy.config.MessageProperties;
import com.selfstudy.modules.applet.annotation.Login;
import com.selfstudy.modules.applet.annotation.LoginUser;
import com.selfstudy.modules.applet.dto.AppointmentBookResult;
import com.selfstudy.modules.applet.dto.save.BasAppointmentSaveDTO;
import com.selfstudy.modules.applet.dto.save.BasMessageSaveDTO;
import com.selfstudy.modules.applet.dto.update.UserInfoUpdateDTO;
import com.selfstudy.modules.applet.entity.UserEntity;
import com.selfstudy.modules.applet.vo.BasAppointmentVO;
import com.selfstudy.modules.bas.entity.*;
import com.selfstudy.modules.bas.service.*;
import com.selfstudy.modules.user.entity.TbUserEntity;
import com.selfstudy.modules.user.service.TbUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.util.StringUtils;

/**
 * 小程序
 *
 * @author kon-foreverkon-forever
 */
@RestController
@RequestMapping("/applet")
@Tag(name = "小程序接口")
public class AppNoticeController {

	private static void trimUserInfo(UserInfoUpdateDTO dto) {
		if (dto.getName() != null) {
			dto.setName(StringUtils.trimWhitespace(dto.getName()));
		}
		if (dto.getMobile() != null) {
			dto.setMobile(StringUtils.trimWhitespace(dto.getMobile()));
		}
		if (dto.getEmail() != null) {
			dto.setEmail(StringUtils.trimWhitespace(dto.getEmail()));
		}
		if (dto.getBz() != null) {
			dto.setBz(StringUtils.trimWhitespace(dto.getBz()));
		}
		if (dto.getUserImg() != null) {
			dto.setUserImg(StringUtils.trimWhitespace(dto.getUserImg()));
		}
	}

    @Autowired
    private MessageProperties messageProperties;
    @Autowired
    private BasNoticeService basNoticeService;
    @Autowired
    private TbUserService tbUserService;
    @Autowired
    private BasFloorService basFloorService;
    @Autowired
    private BasStudyRoomService basStudyRoomService;
    @Autowired
    private BasSeatService basSeatService;
    @Autowired
    private BasAppointmentService basAppointmentService;
    @Autowired
    private BasMessageService basMessageService;
    /**
     * 列表
     */
    @GetMapping("/listNotice")
    @Operation(summary = "公告分页列表")
    public R listNotice(QueryInfoDTO queryInfoDTO){
        PageResult<BasNoticeEntity> pageResult = basNoticeService.listNotice(queryInfoDTO);
        return R.ok().put("data", pageResult);
    }

    @Operation(summary = "查看封禁用户")
    @GetMapping("/listForBan")
    public R listForBan(){
        List<TbUserEntity> voPageResult = tbUserService.listForBan();
        return R.ok().put("data", voPageResult);
    }

    @Operation(summary = "获取全部楼层")
    @GetMapping("/getAllFloor")
    public R getAllFloor(){
        List<BasFloorEntity> list = basFloorService.list(
                new LambdaQueryWrapper<BasFloorEntity>().orderByAsc(BasFloorEntity::getId));
        return R.ok().put("data", list);
    }

    @Operation(summary = "通过楼层获取自习室")
    @GetMapping("/getRoomByFloor")
    public R getRoomByFloor(Long id){
        List<BasStudyRoomEntity> roomEntities = basStudyRoomService.getRoomByFloor(id);
        return R.ok().put("data", roomEntities);
    }

    @Operation(summary = "通过自习室获取座位")
    @GetMapping("/getSeatByRoom")
    public R getSeatByRoom(Long id){
        List<BasSeatEntity> roomEntities = basSeatService.getSeatByRoom(id);
        return R.ok().put("data", roomEntities);
    }


    @Login
    @Operation(summary = "预约前询价（会员免单、槽数、应付）")
    @PostMapping("/appointment/quote")
    public R appointmentQuote(@RequestBody BasAppointmentSaveDTO saveDTO, @RequestAttribute("userId") Long userId) {
        return R.ok().put("data", basAppointmentService.quoteAppointment(saveDTO, userId));
    }

    @Login
    @Operation(summary = "保存预约信息")
    @PostMapping("/appointment")
    public R appointment(@RequestBody BasAppointmentSaveDTO saveDTO,@RequestAttribute("userId") Long userId){
        AppointmentBookResult result = basAppointmentService.appointment(saveDTO, userId);
        if (result.isOk()) {
            return R.ok();
        }
        if (result.isNeedSimulatePay()) {
            return R.error(40210, result.getMessage())
                    .put("amountYuan", result.getAmountYuan())
                    .put("slotCount", result.getSlotCount());
        }
        return R.error(result.getMessage() != null ? result.getMessage() : messageProperties.getFormSaveError());
    }

    @Login
    @Operation(summary = "我的预约信息")
    @PostMapping("/myAppointment")
    public R myAppointment(@RequestAttribute("userId") Long userId){
        List<BasAppointmentVO> basAppointment =  basAppointmentService.myAppointment(userId);
        return R.ok().put("data",basAppointment);
    }

    @Login
    @Operation(summary = "我的预约单条详情")
    @GetMapping("/appointment/{id}")
    public R appointmentDetail(@PathVariable("id") Long id, @RequestAttribute("userId") Long userId) {
        BasAppointmentVO vo = basAppointmentService.getMineById(id, userId);
        if (vo == null) {
            return R.error("记录不存在或无权查看");
        }
        return R.ok().put("data", vo);
    }

    @Login
    @Operation(summary = "取消预约")
    @PostMapping("/cancel")
    public R cancel(@RequestParam("id") Long id, @RequestAttribute("userId") Long userId) {
        basAppointmentService.cancel(id, userId);
        return R.ok();
    }


    @Login
    @Operation(summary = "结束学习")
    @PostMapping("/over")
    public R over(@RequestParam("id") Long id, @RequestAttribute("userId") Long userId) {
        basAppointmentService.over(id, userId);
        return R.ok();
    }

    @Login
    @Operation(summary = "签到（取消超时释放，进入使用中）")
    @PostMapping("/signIn")
    public R signIn(@RequestParam("id") Long id, @RequestAttribute("userId") Long userId) {
        basAppointmentService.signIn(id, userId);
        return R.ok();
    }

    @Login
    @GetMapping("/getUser")
    @Operation(summary = "获取用户信息")
    public R userInfo(@LoginUser UserEntity user){
        return R.ok().put("data", user);
    }

    @Login
    @Operation(summary = "用户信息修改")
    @PostMapping("/userInfo")
    public R userInfo(@RequestAttribute("userId") Long userId, @RequestBody UserInfoUpdateDTO updateDTO){
        trimUserInfo(updateDTO);
        ValidatorUtils.validateEntity(updateDTO);
        TbUserEntity row = tbUserService.getById(userId);
        if (row == null) {
            return R.error("用户不存在");
        }
        BeanUtil.copyProperties(updateDTO, row, CopyOptions.create().setIgnoreNullValue(true));
        row.setUserId(userId);
        boolean update = tbUserService.updateById(row);
        if (update){
            return R.ok();
        }
        return R.error(messageProperties.getFormUpdateError());
    }

    @Login
    @Operation(summary = "用户留言")
    @PostMapping("/userMessage")
    public R userMessage(@LoginUser UserEntity user, @RequestBody BasMessageSaveDTO updateDTO){
        BasMessageEntity basMessageEntity = BeanUtil.copyProperties(updateDTO, BasMessageEntity.class);
        basMessageEntity.setUserId(user.getUserId());
        basMessageEntity.setUsername(user.getUsername());
        boolean save = basMessageService.save(basMessageEntity);
        if (save){
            return R.ok();
        }
        return R.error(messageProperties.getFormSaveError());
    }

    @Operation(summary = "留言列表")
    @GetMapping("/MessageList")
    public R MessageList(){
        LambdaQueryWrapper<BasMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasMessageEntity::getMessageType,1);
        List<BasMessageEntity> pageResult = basMessageService.list(wrapper);
        return R.ok().put("data", pageResult);
    }
}
