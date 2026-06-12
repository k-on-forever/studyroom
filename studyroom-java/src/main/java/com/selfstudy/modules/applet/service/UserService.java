package com.selfstudy.modules.applet.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.applet.entity.UserEntity;
import com.selfstudy.modules.applet.form.LoginForm;
import com.selfstudy.modules.applet.form.WXLoginForm;

/**
 * 用户
 *
 * @author kon-foreverkon-forever
 */
public interface UserService extends IService<UserEntity> {

	UserEntity queryByAccount(String mobile);

	/**
	 * 用户登录
	 * @param form    登录表单
	 * @return        返回用户ID
	 */
	R login(LoginForm form);

	UserEntity queryUser(Long userId);

	R logout(Long userId);

	/**
	 * 微信小程序登录
	 * @param wxLoginForm
	 * @return
	 */
	R wxLogin(WXLoginForm wxLoginForm);

	/** 已通过短信校验后更新密码（明文） */
	boolean resetPassword(String account, String newPlainPassword);

}
