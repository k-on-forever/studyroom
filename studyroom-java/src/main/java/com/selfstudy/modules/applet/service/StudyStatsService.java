package com.selfstudy.modules.applet.service;

import com.selfstudy.modules.applet.dto.StudyStatsDTO;

public interface StudyStatsService {

	StudyStatsDTO statsForUser(Long userId);
}
