# 生产环境：微信支付、短信、订阅消息

当前代码中包含 **模拟支付**（会员订单、按次预约）、**模拟短信**（注册 / 找回密码）。上线前请按下述方案替换为正式能力，并与 **`application-prod`** 配置分离。

---

## 1. 微信支付（小程序）

**涉及能力**

- 会员卡：`AppMembershipController` 中 `createPendingOrder`、`mockPay`（模拟）→ 应改为 **小程序支付**：统一下单、`wx.requestPayment`、支付结果回调、订单状态更新。  
- 按次预约：`BasAppointmentServiceImpl` 中 **40210** 返回「需支付」→ 小程序 **`mock-pay`** 页应改为调起真实支付，支付成功后再带标记调用 `/applet/appointment`。

**建议步骤**

1. 开通微信商户号、小程序绑定商户号，配置 API 密钥与证书（v2/v3 按官方文档）。  
2. 服务端实现 **支付回调 URL**（HTTPS、公网可访问），校验签名、幂等更新 `bas_membership_order` / 预约单。  
3. 关闭或权限收紧 **`/applet/membership/mockPay`** 等模拟接口（可用 **`spring.profiles`** 仅在 `dev` 注册）。  
4. 对账：每日与微信账单核对 **`bas_membership_order`**、预约 **`pay_amount`**。

**配置项（建议新增到独立配置类，勿提交密钥）**

- `wx.miniapp.appid` / `secret`（已有占位）  
- `wx.pay.mchId`、`apiV3Key`、`privateKeyPath`、`serialNo`、`notifyUrl`

---

## 2. 短信（注册 / 找回密码）

**当前行为**：验证码写入日志或进程内存储（`AppSmsController`、`AppletRegisterSmsCodeStore`、`AppPasswordController`）。

**上线建议**

- 接入腾讯云短信 / 阿里云短信等，模板审核通过后替换发送逻辑。  
- **频率限制**：同一手机号每日次数、IP 限流（防刷）。  
- **验证码**：仍建议 Redis 存储 TTL + 单次消费（与图形验证码策略一致）。

---

## 3. 订阅消息（预约提醒等）

**场景示例**

- 预约成功、开始前 N 分钟提醒、取消通知。

**步骤**

1. 微信公众平台 → 功能 → 订阅消息，选用模板，记录 **模板 ID**。  
2. 小程序端：在合适时机 **`wx.requestSubscribeMessage`** 获取用户授权。  
3. 服务端：定时任务或预约变更时调用 **发送订阅消息** API（`access_token`、openid、模板数据）。  
4. 将 **`openid`** 与 `tb_user` 关联（若当前仅用帐号密码登录，需补充微信登录或绑定 openid）。

**配置建议**

- `wx.subscribe.template.bookingSuccess` 等键放在配置文件或 DB，便于更换模板。

---

## 4. 安全与合规

- 所有密钥使用 **环境变量** 或密钥管理服务，禁止写入 Git。  
- 回调 URL **仅 HTTPS**，校验微信签名。  
- 日志中 **禁止** 打印支付密钥、用户身份证、完整手机号（需脱敏）。

---

## 5. 与开发环境的切换

- **`study.wx.pay.mode=mock`**（默认）：小程序走「演示支付」→ `/applet/wxpay/mock/confirm`，**禁止**客户端伪造 `simulatePaidNonMember`。  
- **`study.wx.pay.mode=wx`**：须配置环境变量并执行迁移 **`schema-studyroom-migration-wx-pay-order.sql`**：

| 变量 | 说明 |
|------|------|
| `STUDY_WX_PAY_MCH_ID` | 商户号 |
| `STUDY_WX_PAY_API_V3_KEY` | APIv3 密钥 |
| `STUDY_WX_PAY_PRIVATE_KEY_PATH` | 商户私钥 apiclient_key.pem 绝对路径 |
| `STUDY_WX_PAY_SERIAL` | 商户证书序列号 |
| `STUDY_WX_PAY_NOTIFY_URL` | 公网 HTTPS，如 `https://你的域名/self-study/applet/wxpay/notify` |

用户须 **微信登录**（`tb_user.open_id`）方可 JSAPI 支付。`applet.wechat.appid` / `secret` 与商户号绑定的小程序一致。

时段粒度：执行 **`schema-studyroom-migration-slot-step-10-align.sql`**，并保持 `study.reservation.slot-minutes: 10` 与小程序选座栅格一致。

浏览暂锁：默认 **`study.browse-lock.enabled: false`**（已关闭 Redis 浏览占位）。

当前仓库以 **`spring.profiles.active=dev`** 为默认；生产部署时改为 **`prod`** 并加载 **`application-prod.yml`**（或由运维平台注入等价属性）。
