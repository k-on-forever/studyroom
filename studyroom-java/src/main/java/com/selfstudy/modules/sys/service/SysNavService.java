package com.selfstudy.modules.sys.service;

import java.util.List;
import java.util.Map;

public interface SysNavService {

	Map<String, Object> buildNavPayload(Long adminId);
}
