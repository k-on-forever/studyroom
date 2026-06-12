package com.selfstudy.modules.applet.controller;


import com.selfstudy.common.utils.R;
import com.selfstudy.common.validator.ValidatorUtils;
import com.selfstudy.config.MessageProperties;
import com.selfstudy.modules.applet.entity.UserEntity;
import com.selfstudy.modules.applet.form.RegisterForm;
import com.selfstudy.modules.applet.service.UserService;
import com.selfstudy.modules.applet.support.AppletRegisterSmsCodeStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 注册
 *
 * @author kon-foreverkon-forever
 */
@RestController
@RequestMapping("/applet")
@Tag(name = "小程序注册接口")
public class AppRegisterController {
	private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    @Autowired
    private UserService userService;
    @Autowired
    private MessageProperties messageProperties;
    @Autowired
    private AppletRegisterSmsCodeStore smsCodeStore;

    @PostMapping("register")
    @Operation(summary = "注册")
    public R register(@RequestBody RegisterForm form){
        //表单校验
        ValidatorUtils.validateEntity(form);

        String account = form.getAccount().trim();
        if (!PHONE.matcher(account).matches()) {
            return R.error("请使用中国大陆手机号注册");
        }
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            return R.error("两次输入的密码不一致");
        }
        if (!smsCodeStore.verifyAndConsume(account, form.getSmsCode())) {
            return R.error("验证码错误或已过期");
        }
        if (userService.queryByAccount(account) != null) {
            return R.error("该账号已注册");
        }

        UserEntity user = new UserEntity();
        user.setAccount(account);
        user.setUsername("用户_"+UUID.randomUUID().toString().substring(0,8));
        user.setPassword(DigestUtils.sha256Hex(form.getPassword()));
        user.setCreateTime(new Date());
        user.setMobile(account);
        boolean save = userService.save(user);
        if (save){
            return R.ok();
        }

        return R.error(messageProperties.getFormSaveError());
    }
}
