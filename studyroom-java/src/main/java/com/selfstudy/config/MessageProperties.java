package com.selfstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "study.message")
public class MessageProperties {

	private String formSaveError = "保存失败";
	private String formUpdateError = "更新失败";
	private String userAccountExist = "账号或密码错误";

	public String getFormSaveError() {
		return formSaveError;
	}

	public void setFormSaveError(String formSaveError) {
		this.formSaveError = formSaveError;
	}

	public String getFormUpdateError() {
		return formUpdateError;
	}

	public void setFormUpdateError(String formUpdateError) {
		this.formUpdateError = formUpdateError;
	}

	public String getUserAccountExist() {
		return userAccountExist;
	}

	public void setUserAccountExist(String userAccountExist) {
		this.userAccountExist = userAccountExist;
	}
}
