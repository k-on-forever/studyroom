package com.selfstudy.modules.bas.service;

import java.util.List;
import java.util.Map;

public interface AdminAnalyticsService {

	Map<String, Object> overviewForBizDate(String bizDate);

	Map<String, Object> todayRevenue();

	List<Map<String, Object>> revenueLastDays(int days);

	List<Map<String, Object>> revenueByWeeks(int pastWeeks);

	List<Map<String, Object>> revenueByMonths(int pastMonths);

	Map<String, Object> utilization(String fromDate, String toDate, int topSeats);
}
