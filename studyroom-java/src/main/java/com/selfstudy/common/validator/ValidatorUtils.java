package com.selfstudy.common.validator;

import com.selfstudy.common.exception.RRException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

public final class ValidatorUtils {

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	private ValidatorUtils() {
	}

	public static void validateEntity(Object object, Class<?>... groups) {
		Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(object, groups);
		if (!violations.isEmpty()) {
			ConstraintViolation<Object> first = violations.iterator().next();
			throw new RRException(first.getMessage());
		}
	}
}
