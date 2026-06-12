package com.selfstudy.modules.applet.controller;


import com.selfstudy.common.utils.R;
import com.selfstudy.common.validator.ValidatorUtils;
import com.selfstudy.modules.applet.annotation.Login;
import com.selfstudy.modules.applet.form.LoginForm;
import com.selfstudy.modules.applet.form.WXLoginForm;
import com.selfstudy.modules.applet.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序登录接口
 *
 * @author kon-foreverkon-forever
 */
@Validated
@RestController
@RequestMapping("/applet")
@Tag(name = "小程序登录接口")
public class AppLoginController {
    @Autowired
    private UserService userService;

    /**
     * 微信小程序登录
     * @param wxLoginForm
     * @return
     */
    @PostMapping("/wxLogin")
    @Operation(summary = "微信小程序登录")
    public R wxLogin(@RequestBody @Validated WXLoginForm wxLoginForm) {
        return userService.wxLogin(wxLoginForm);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    public R login(@RequestBody LoginForm form){
        ValidatorUtils.validateEntity(form);
        return userService.login(form);
    }

    @Login
    @PostMapping("/logOut")
    @Operation(summary = "退出登录 不需要传参")
    public R logout(@RequestAttribute("userId") Long userId){
        return userService.logout(userId);
    }
}
