package com.selfstudy.common.exception;

import com.selfstudy.common.utils.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 统一 JSON 异常输出：降低空白 500、校验错误可读性；业务异常 {@link RRException} 仍为 HTTP 200 + body.code（与人人前端约定一致）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Value("${study.exception.expose-detail:false}")
	private boolean exposeDetail;

	@ExceptionHandler(RRException.class)
	public ResponseEntity<R> handleRRException(RRException e) {
		return ResponseEntity.ok(R.error(e.getCode(), e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<R> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
		String msg = e.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.filter(m -> m != null && !m.isEmpty())
				.findFirst()
				.orElse("参数校验失败");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.error(400, msg));
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<R> handleBind(BindException e) {
		String msg = e.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.filter(m -> m != null && !m.isEmpty())
				.findFirst()
				.orElse("参数绑定失败");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.error(400, msg));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<R> handleConstraintViolation(ConstraintViolationException e) {
		String msg = e.getConstraintViolations().stream()
				.map(ConstraintViolation::getMessage)
				.collect(Collectors.joining("；"));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.error(400, msg.isEmpty() ? "参数不合法" : msg));
	}

	@ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class })
	public ResponseEntity<R> handleBadRequest(Exception e) {
		String msg = e.getMessage() != null ? e.getMessage() : "请求参数错误";
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.error(400, msg));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<R> handleIllegalArgument(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.error(400, e.getMessage() != null ? e.getMessage() : "参数不合法"));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<R> handleDataAccess(DataAccessException e) {
		log.error("数据库访问异常", e);
		String msg = exposeDetail && e.getMessage() != null
				? "数据访问失败：" + truncate(e.getMessage(), 200)
				: "数据访问失败，请稍后重试或联系管理员";
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.error(msg));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<R> handleAny(Exception e) {
		log.error("未捕获异常", e);
		String msg = "系统繁忙，请稍后重试";
		if (exposeDetail && e.getMessage() != null) {
			msg = e.getClass().getSimpleName() + ": " + truncate(e.getMessage(), 400);
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.error(msg));
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return "";
		}
		return s.length() <= max ? s : s.substring(0, max) + "…";
	}
}
