package com.selfstudy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地上传目录（头像等）。生产建议配置为绝对路径或挂载卷。
 */
@Data
@Component
@ConfigurationProperties(prefix = "study.upload")
public class StudyUploadProperties {

	/** 相对路径时相对进程工作目录（user.dir） */
	private String avatarDir = "data/upload/avatar";

	public Path resolveAvatarDir() {
		Path p = Paths.get(avatarDir.trim());
		return p.isAbsolute() ? p.normalize() : Paths.get(System.getProperty("user.dir", ".")).resolve(p).normalize().toAbsolutePath();
	}
}
