package com.selfstudy.modules.applet.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.config.StudyUploadProperties;
import com.selfstudy.modules.applet.annotation.Login;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * 小程序头像上传（本地落盘 + 可匿名 GET 访问 {@code /upload/avatar/**}）。
 */
@RestController
@RequestMapping("/applet/upload")
@Tag(name = "小程序-上传")
public class AppAvatarUploadController {

	private static final long MAX_BYTES = 2 * 1024 * 1024;

	private final StudyUploadProperties uploadProperties;

	public AppAvatarUploadController(StudyUploadProperties uploadProperties) {
		this.uploadProperties = uploadProperties;
	}

	@Login
	@PostMapping("/avatar")
	@Operation(summary = "上传用户头像（覆盖同用户旧文件）")
	public R uploadAvatar(@RequestAttribute("userId") Long userId, @RequestParam("file") MultipartFile file,
			HttpServletRequest request) throws IOException {
		if (file == null || file.isEmpty()) {
			return R.error("请选择图片");
		}
		if (file.getSize() > MAX_BYTES) {
			return R.error("图片不可超过 2MB");
		}
		String ct = file.getContentType();
		if (ct == null || !ct.toLowerCase(Locale.ROOT).startsWith("image/")) {
			return R.error("仅支持图片文件");
		}
		String ext = pickExtension(ct);
		Path dir = uploadProperties.resolveAvatarDir();
		Files.createDirectories(dir);
		deleteUserAvatarVariants(dir, userId);
		Path dest = dir.resolve(userId + ext);
		file.transferTo(dest.toFile());

		String url = buildPublicUrl(request, userId, ext);
		return R.ok().put("userImg", url);
	}

	private static void deleteUserAvatarVariants(Path dir, Long userId) throws IOException {
		if (!Files.isDirectory(dir)) {
			return;
		}
		String sid = Objects.requireNonNull(userId).toString() + ".";
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir,
				p -> p.getFileName() != null && p.getFileName().toString().startsWith(sid))) {
			for (Path p : stream) {
				Files.deleteIfExists(p);
			}
		}
	}

	private static String pickExtension(String contentType) {
		String c = contentType.toLowerCase(Locale.ROOT);
		if (c.contains("png")) {
			return ".png";
		}
		if (c.contains("webp")) {
			return ".webp";
		}
		return ".jpg";
	}

	private static String buildPublicUrl(HttpServletRequest request, Long userId, String ext) {
		String scheme = request.getScheme();
		String host = request.getServerName();
		int port = request.getServerPort();
		StringBuilder sb = new StringBuilder();
		sb.append(scheme).append("://").append(host);
		if (port > 0 && !("http".equalsIgnoreCase(scheme) && port == 80)
				&& !("https".equalsIgnoreCase(scheme) && port == 443)) {
			sb.append(":").append(port);
		}
		String ctx = request.getContextPath() != null ? request.getContextPath() : "";
		sb.append(ctx).append("/upload/avatar/").append(userId).append(ext);
		return sb.toString();
	}
}
