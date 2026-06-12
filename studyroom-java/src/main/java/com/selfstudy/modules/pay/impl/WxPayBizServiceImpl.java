package com.selfstudy.modules.pay.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyV3Result;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.selfstudy.common.exception.RRException;
import com.selfstudy.config.StudyWxPayProperties;
import com.selfstudy.modules.applet.dto.save.BasAppointmentSaveDTO;
import com.selfstudy.modules.applet.entity.UserEntity;
import com.selfstudy.modules.applet.service.UserService;
import com.selfstudy.modules.bas.entity.BasMembershipOrderEntity;
import com.selfstudy.modules.bas.entity.BasWxPayOrderEntity;
import com.selfstudy.modules.bas.service.BasAppointmentService;
import com.selfstudy.modules.bas.service.BasMembershipOrderService;
import com.selfstudy.modules.bas.dao.BasWxPayOrderDao;
import com.selfstudy.modules.pay.WxPayBizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WxPayBizServiceImpl implements WxPayBizService {

	private static final String BIZ_APPOINTMENT = "APPOINTMENT";
	private static final String BIZ_MEMBERSHIP = "MEMBERSHIP";
	private static final int ST_PENDING = 0;
	private static final int ST_PAID = 1;
	private static final int ST_CONSUMED = 2;

	private final StudyWxPayProperties payProps;
	private final BasWxPayOrderDao wxPayOrderDao;
	private final BasAppointmentService basAppointmentService;
	private final BasMembershipOrderService basMembershipOrderService;
	private final UserService userService;
	private final String miniAppId;

	@Autowired(required = false)
	private WxPayService wxPayService;

	public WxPayBizServiceImpl(StudyWxPayProperties payProps, BasWxPayOrderDao wxPayOrderDao,
			@Lazy BasAppointmentService basAppointmentService,
			BasMembershipOrderService basMembershipOrderService, UserService userService,
			@Value("${applet.wechat.appid:}") String miniAppId) {
		this.payProps = payProps;
		this.wxPayOrderDao = wxPayOrderDao;
		this.basAppointmentService = basAppointmentService;
		this.basMembershipOrderService = basMembershipOrderService;
		this.userService = userService;
		this.miniAppId = miniAppId;
	}

	@Override
	public Map<String, Object> clientPayConfig() {
		Map<String, Object> m = new HashMap<>();
		m.put("payMode", payProps.isMockMode() ? "mock" : "wx");
		m.put("mockPayAllowed", payProps.isMockMode());
		m.put("wxPayReady", !payProps.isMockMode() && payProps.isWxConfigured() && wxPayService != null);
		return m;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> prepayAppointment(Long userId, BasAppointmentSaveDTO saveDTO) {
		Map<String, Object> quote = basAppointmentService.quoteAppointment(saveDTO, userId);
		boolean memberFree = Boolean.TRUE.equals(quote.get("memberFree"));
		BigDecimal amount = quote.get("amountYuan") instanceof BigDecimal
				? (BigDecimal) quote.get("amountYuan")
				: new BigDecimal(String.valueOf(quote.get("amountYuan")));
		if (memberFree || amount.compareTo(BigDecimal.ZERO) <= 0) {
			Map<String, Object> free = new HashMap<>();
			free.put("needPay", false);
			free.put("memberFree", memberFree);
			return free;
		}
		int fen = yuanToFen(amount);
		String outTradeNo = newOutTradeNo();
		BasWxPayOrderEntity row = new BasWxPayOrderEntity();
		row.setUserId(userId);
		row.setBizType(BIZ_APPOINTMENT);
		row.setOutTradeNo(outTradeNo);
		row.setAmountFen(fen);
		row.setStatus(ST_PENDING);
		row.setPayloadJson(JSON.toJSONString(saveDTO));
		row.setCreateTime(new Date());
		wxPayOrderDao.insert(row);
		if (payProps.isMockMode()) {
			Map<String, Object> mock = new HashMap<>();
			mock.put("needPay", true);
			mock.put("mock", true);
			mock.put("outTradeNo", outTradeNo);
			mock.put("amountYuan", amount);
			return mock;
		}
		return buildWxJsapi(userId, outTradeNo, fen, "自习室座位预约");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> prepayMembership(Long userId, Long membershipOrderId) {
		BigDecimal amount = basMembershipOrderService.pendingAmount(userId, membershipOrderId);
		if (amount == null) {
			throw new RRException("订单不存在或已支付");
		}
		int fen = yuanToFen(amount);
		String outTradeNo = newOutTradeNo();
		BasWxPayOrderEntity row = new BasWxPayOrderEntity();
		row.setUserId(userId);
		row.setBizType(BIZ_MEMBERSHIP);
		row.setBizRef(String.valueOf(membershipOrderId));
		row.setOutTradeNo(outTradeNo);
		row.setAmountFen(fen);
		row.setStatus(ST_PENDING);
		row.setCreateTime(new Date());
		wxPayOrderDao.insert(row);
		if (payProps.isMockMode()) {
			Map<String, Object> mock = new HashMap<>();
			mock.put("needPay", true);
			mock.put("mock", true);
			mock.put("outTradeNo", outTradeNo);
			mock.put("amountYuan", amount);
			mock.put("orderId", membershipOrderId);
			return mock;
		}
		return buildWxJsapi(userId, outTradeNo, fen, "自习室会员卡");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void mockMarkPaid(Long userId, String outTradeNo) {
		if (!payProps.isMockMode()) {
			throw new RRException("当前环境须使用微信支付");
		}
		BasWxPayOrderEntity row = loadPending(userId, outTradeNo);
		row.setStatus(ST_PAID);
		row.setPaidTime(new Date());
		row.setWxTransactionId("MOCK-" + outTradeNo);
		wxPayOrderDao.updateById(row);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void assertAppointmentPaid(Long userId, String outTradeNo, BigDecimal amountYuan) {
		if (!StringUtils.hasText(outTradeNo)) {
			throw new RRException("请先完成支付");
		}
		BasWxPayOrderEntity row = wxPayOrderDao.selectOne(new LambdaQueryWrapper<BasWxPayOrderEntity>()
				.eq(BasWxPayOrderEntity::getOutTradeNo, outTradeNo.trim()));
		if (row == null || !userId.equals(row.getUserId())) {
			throw new RRException("支付单不存在");
		}
		if (!BIZ_APPOINTMENT.equals(row.getBizType())) {
			throw new RRException("支付单类型不匹配");
		}
		if (row.getStatus() == null || row.getStatus() != ST_PAID) {
			throw new RRException("支付未完成，请稍候或重新支付");
		}
		int expectedFen = yuanToFen(amountYuan);
		if (row.getAmountFen() == null || row.getAmountFen() != expectedFen) {
			throw new RRException("支付金额与订单不一致，请重新询价");
		}
		row.setStatus(ST_CONSUMED);
		wxPayOrderDao.updateById(row);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void completeMembershipFromPay(String outTradeNo) {
		BasWxPayOrderEntity row = wxPayOrderDao.selectOne(new LambdaQueryWrapper<BasWxPayOrderEntity>()
				.eq(BasWxPayOrderEntity::getOutTradeNo, outTradeNo));
		if (row == null || row.getStatus() == null || row.getStatus() != ST_PAID) {
			return;
		}
		if (!BIZ_MEMBERSHIP.equals(row.getBizType()) || !StringUtils.hasText(row.getBizRef())) {
			return;
		}
		Long orderId = Long.parseLong(row.getBizRef());
		basMembershipOrderService.mockPayOrder(row.getUserId(), orderId);
		row.setStatus(ST_CONSUMED);
		wxPayOrderDao.updateById(row);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public String handlePayNotify(String body, String signature, String nonce, String timestamp, String serial) {
		if (wxPayService == null) {
			return failXml();
		}
		try {
			SignatureHeader header = new SignatureHeader();
			header.setSignature(signature);
			header.setNonce(nonce);
			header.setTimeStamp(timestamp);
			header.setSerial(serial);
			WxPayNotifyV3Result notifyResult = wxPayService.parseOrderNotifyV3Result(body, header);
			WxPayNotifyV3Result.DecryptNotifyResult result = notifyResult.getResult();
			if (result == null) {
				return failXml();
			}
			String outTradeNo = result.getOutTradeNo();
			String transactionId = result.getTransactionId();
			markPaid(outTradeNo, transactionId);
			return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
		} catch (WxPayException e) {
			return failXml();
		}
	}

	private void markPaid(String outTradeNo, String transactionId) {
		BasWxPayOrderEntity row = wxPayOrderDao.selectOne(new LambdaQueryWrapper<BasWxPayOrderEntity>()
				.eq(BasWxPayOrderEntity::getOutTradeNo, outTradeNo));
		if (row == null || row.getStatus() == null || row.getStatus() != ST_PENDING) {
			return;
		}
		row.setStatus(ST_PAID);
		row.setPaidTime(new Date());
		row.setWxTransactionId(transactionId);
		wxPayOrderDao.updateById(row);
		if (BIZ_MEMBERSHIP.equals(row.getBizType())) {
			completeMembershipFromPay(outTradeNo);
		}
	}

	private Map<String, Object> buildWxJsapi(Long userId, String outTradeNo, int totalFen, String description) {
		if (!payProps.isWxConfigured() || wxPayService == null) {
			throw new RRException("微信支付未配置，请联系门店或检查 study.wx.pay 与商户证书");
		}
		String openid = requireOpenId(userId);
		try {
			WxPayUnifiedOrderV3Request req = new WxPayUnifiedOrderV3Request();
			req.setAppid(miniAppId);
			req.setMchid(payProps.getMchId());
			req.setDescription(description);
			req.setOutTradeNo(outTradeNo);
			req.setNotifyUrl(payProps.getNotifyUrl());
			WxPayUnifiedOrderV3Request.Amount amount = new WxPayUnifiedOrderV3Request.Amount();
			amount.setTotal(totalFen);
			amount.setCurrency("CNY");
			req.setAmount(amount);
			WxPayUnifiedOrderV3Request.Payer payer = new WxPayUnifiedOrderV3Request.Payer();
			payer.setOpenid(openid);
			req.setPayer(payer);
			WxPayUnifiedOrderV3Result.JsapiResult jsapi = wxPayService.createOrderV3(TradeTypeEnum.JSAPI, req);
			Map<String, Object> data = new HashMap<>();
			data.put("needPay", true);
			data.put("mock", false);
			data.put("outTradeNo", outTradeNo);
			data.put("timeStamp", jsapi.getTimeStamp());
			data.put("nonceStr", jsapi.getNonceStr());
			data.put("package", jsapi.getPackageValue());
			data.put("signType", jsapi.getSignType());
			data.put("paySign", jsapi.getPaySign());
			return data;
		} catch (WxPayException e) {
			throw new RRException("微信下单失败：" + e.getMessage());
		}
	}

	private String requireOpenId(Long userId) {
		UserEntity u = userService.queryUser(userId);
		if (u == null || !StringUtils.hasText(u.getOpenId())) {
			throw new RRException("请使用微信登录后再支付（需获取 openid）");
		}
		return u.getOpenId().trim();
	}

	private BasWxPayOrderEntity loadPending(Long userId, String outTradeNo) {
		BasWxPayOrderEntity row = wxPayOrderDao.selectOne(new LambdaQueryWrapper<BasWxPayOrderEntity>()
				.eq(BasWxPayOrderEntity::getOutTradeNo, outTradeNo));
		if (row == null || !userId.equals(row.getUserId())) {
			throw new RRException("支付单不存在");
		}
		if (row.getStatus() == null || row.getStatus() != ST_PENDING) {
			throw new RRException("支付单状态不可操作");
		}
		return row;
	}

	private static String newOutTradeNo() {
		return "SR" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
	}

	private static int yuanToFen(BigDecimal yuan) {
		return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void afterClientPaid(Long userId, String outTradeNo) {
		if (!StringUtils.hasText(outTradeNo)) {
			return;
		}
		BasWxPayOrderEntity row = wxPayOrderDao.selectOne(new LambdaQueryWrapper<BasWxPayOrderEntity>()
				.eq(BasWxPayOrderEntity::getOutTradeNo, outTradeNo.trim()));
		if (row == null || !userId.equals(row.getUserId()) || row.getStatus() == null || row.getStatus() != ST_PAID) {
			return;
		}
		if (BIZ_MEMBERSHIP.equals(row.getBizType())) {
			completeMembershipFromPay(outTradeNo.trim());
		}
	}

	private static String failXml() {
		return "{\"code\":\"FAIL\",\"message\":\"失败\"}";
	}
}
