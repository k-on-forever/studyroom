package com.selfstudy.modules.bas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.common.base.PageResult;
import com.selfstudy.modules.applet.dto.AppointmentBookResult;
import com.selfstudy.modules.applet.dto.save.BasAppointmentSaveDTO;
import com.selfstudy.modules.applet.vo.BasAppointmentVO;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.vo.BasAppointmentAdminVO;

import java.util.List;
import java.util.Map;

public interface BasAppointmentService extends IService<BasAppointmentEntity> {
	AppointmentBookResult appointment(BasAppointmentSaveDTO saveDTO, Long userId);

	/** 预约前询价：是否会员免单、槽数、应付金额 */
	Map<String, Object> quoteAppointment(BasAppointmentSaveDTO saveDTO, Long userId);

	List<BasAppointmentVO> myAppointment(Long userId);

	/** 当前用户的单条预约详情（非本人返回 null） */
	BasAppointmentVO getMineById(Long id, Long userId);

	Boolean cancel(Long id, Long userId);

	Boolean over(Long id, Long userId);

	/** 从 DB 有效预约重建 Redis 座位位图 */
	int rebuildSeatBitmapsFromDb();

	/** 签到：取消超时释放任务并标记已签到 */
	boolean signIn(Long appointmentId, Long userId);

	/** 调度器：对仍为待签到的记录执行爽约处理 */
	void processNoShowIfNeeded(Long appointmentId);

	/** 调度器：预约时段已结束且未手动签退/取消时，自动释放座位 */
	void processSessionEndIfNeeded(Long appointmentId);

	PageResult<BasAppointmentAdminVO> adminPage(long page, long limit, String bizDate, Integer seatState, String keyword);

	/** 管理员强制取消（待签到/使用中），释放占用 */
	boolean adminCancel(Long id);

	/** 导出：与 adminPage 相同筛选，最多 5000 条 */
	List<BasAppointmentAdminVO> adminExport(String bizDate, Integer seatState, String keyword);
}
