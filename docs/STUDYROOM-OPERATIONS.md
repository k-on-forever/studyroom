# 自习室项目：运维与本地开发说明

本文覆盖：**数据库迁移顺序**、**本地 / 生产配置**、**HTTPS**、以及仓库内已实现能力的索引（全局异常、导出、权限注解、审计日志切面）。

- 生产配置模板：`studyroom-java/src/main/resources/application-prod.example.yml`
- 对接清单（支付 / 短信 / 订阅消息）：`docs/PRODUCTION-INTEGRATIONS.md`

---

## 1. 数据库迁移清单（推荐顺序）

仅补「三层楼 + A/B/C 自习室 + 示例座位」可执行：**`studyroom-java/src/main/resources/db/schema-studyroom-seed-three-floors.sql`**（与 `core.sql` 中示例一致，可单文件重复执行）。

新库从零初始化：

1. **`studyroom-java/src/main/resources/db/schema-studyroom-core.sql`**  
   建库表、基础菜单、`sys_admin` 默认账号、`sys_reservation_rule` 等。

已有库、版本较旧时，在业务库中按需执行（**已存在的表 / 列会报错，可跳过该语句**）：

| 顺序 | 脚本 | 用途 |
|------|------|------|
| A | `schema-studyroom-migration-v2-bas-reservation.sql` | 楼层/座位网格、`sys_reservation_rule` 等 |
| B | `schema-studyroom-migration-v3-membership-room-slot.sql` | 会员卡表、`slot_step_minutes` 等 |
| C | `schema-studyroom-migration-v4-membership-order-appointment-pay.sql` | 订单表、`pay_amount`/`pay_status` |
| D | `schema-studyroom-migration-bas-appointment-version.sql` | `version` 乐观锁 |
| E | `schema-studyroom-migration-appointment-times.sql` | `check_in_at`/`study_end_at` |
| F | `schema-studyroom-migration-admin-platform.sql` | 会员卡学时、`seat_type`、钱包与优惠券表 |
| G | `schema-studyroom-migration-sys-reservation-rule.sql` | 仅预约规则表（若 core 未执行） |
| H | `schema-studyroom-migration-bas-seat-seat-type.sql` | 仅 `bas_seat.seat_type` |
| I | `schema-studyroom-migration-bas-appointment-pay-columns.sql` | 仅预约计价列 |
| J | `schema-studyroom-migration-mini-api-db-align.sql` | 小程序 / 统计接口列对齐（汇总补丁） |
| K | `schema-studyroom-migration-bas-study-room-close-2230.sql` | 将 `bas_study_room.close_time` 从 22:00 更正为 **22:30**（与预约全局营业结束一致） |
| K2 | `schema-studyroom-migration-global-hourly-slot-60.sql` | 自习室 `slot_step_minutes` 改为 **60**（按小时预约，与默认 `study.reservation.slot-minutes` 一致） |
| K3 | `schema-studyroom-migration-bas-seat-access-rule.sql` | 座位**时段策略**（后台锁座 / 仅白名单可订）；管理端「座位管理 → 时段策略」 |
| K3b | `schema-studyroom-migration-bas-seat-access-rule-whitelist-mobile.sql` | 白名单列注释（手机号 JSON）；新建库若已含 K3 可跳过 |
| L | 其它 `schema-studyroom-migration-*.sql` | 按文件名说明（如 QQ bigint、楼层种子数据等） |
| M | `schema-studyroom-migration-hot-path-indexes.sql` | `tb_token` / `bas_appointment` / `bas_seat` / `bas_study_room` 热路径索引（列表与鉴权） |
| N | `schema-studyroom-migration-membership-activate-change-count.sql` | 会员卡 `activate_change_count`（每张期限卡激活日最多修改 3 次） |
| P | `schema-studyroom-migration-wx-pay-order.sql` | 微信支付单 `bas_wx_pay_order` |
| Q | `schema-studyroom-migration-slot-step-10-align.sql` | 自习室 `slot_step_minutes` 统一为 10（与 `study.reservation.slot-minutes` 一致） |
| O | `schema-studyroom-migration-expand-floors-20seats.sql` | 1F/2F/3F 每间自习室 **4×5=20 座**（保留原 id 1–10，新增至 60） |

执行完迁移后**重启** `studyroom-java`。

**Redis 座位位图漂移**：管理端登录后 `POST /self-study/sys/bas/appointment/rebuild-bitmaps`（需 `bas:appointment:list` 权限），按库内「待签到/使用中」预约重建位图。若出现 `Unknown column`，对照 **`schema-studyroom-core.sql`** 与上述补丁补列。

---

## 2. 本地开发配置

| 组件 | 说明 |
|------|------|
| **JDK** | 与工程一致（例：Java 22） |
| **MySQL** | `application-dev.yml` 中 `spring.datasource.druid.url` 指向本机或局域网库 |
| **Redis** | `application.yml` 中 `spring.data.redis`；本地可不启 Redis 时验证码可走内存（`study.captcha.memory-fallback: true`），但预约占座等仍建议 Redis 可用 |
| **端口** | 默认后端 **`server.port: 8081`**，`context-path: /self-study` |
| **管理端** | `studyroom-vue`：`npm run dev`，Node **18+**（推荐 **22**，见 `studyroom-vue/.nvmrc`）；`static/config/index.js` 中 **`baseUrl`** 须与后端端口一致 |
| **小程序** | `studyroom-wx/utils/config.js` 中 **`baseUrl`**；真机调试改为电脑局域网 IP（详见 **`docs/STUDYROOM-MINIPROGRAM-DEVICE.md`**） |

敏感默认值可通过环境变量覆盖（见下一节）。

---

## 3. 生产配置外置（环境变量）

不要在仓库中提交真实生产密钥。`application.yml` / `application-dev.yml` 中已支持 **`${ENV_VAR:默认值}`** 形式（详见文件内注释）。

上线时请至少设置：

- **`SPRING_DATASOURCE_URL`** / **`SPRING_DATASOURCE_USERNAME`** / **`SPRING_DATASOURCE_PASSWORD`**（或通过 Spring Boot 标准 `SPRING_DATASOURCE_*`）
- **`SPRING_DATA_REDIS_HOST`**、**`SPRING_DATA_REDIS_PASSWORD`**
- **`STUDY_ADMIN_JWT_SECRET`**、**`APPLET_JWT_SECRET`**（与默认必须不同）
- **`study.captcha.memory-fallback=false`**（多节点时必须 Redis 存验证码）
- **`SPRINGDOC_API_DOCS_ENABLED=false`**、**`SPRINGDOC_SWAGGER_UI_ENABLED=false`**（或于 `application-prod` 中关闭 `springdoc`）
- **`study.cors.allowed-origin-patterns`**：填写管理端正式域名（勿 `*` + `allow-credentials: true`）

启动 **`prod` profile** 时，若仍使用默认 JWT，日志会输出 **`【安全】`** 错误提示。

完整生产示例模板：**`studyroom-java/src/main/resources/application-prod.example.yml`**（复制为 `application-prod.yml` 或合并进部署配置，**勿提交含真实密码的文件**）。

### 自动化测试

```bash
cd studyroom-java
mvn test
```

默认已开启 Surefire（`skipTests=false`）。本地临时跳过：`mvn test -DskipTests=true`。

---

## 4. HTTPS（生产）

- **反向代理**：推荐 Nginx / 云负载均衡终止 TLS，后端仍监听 HTTP（内网），由代理加 `X-Forwarded-*` 头。  
- **Spring Boot 直连 TLS**：在 `server.ssl.*` 配置证书（示例见 `application-prod.example.yml` 注释）。  
- **微信小程序**：正式版请求域名必须在微信公众平台配置 **HTTPS** 合法域名。

---

## 5. 全局异常与排障

- **`GlobalExceptionHandler`** 会捕获校验异常、参数绑定错误、数据访问异常及通用 `Exception`，返回 **JSON**（`R` 结构），避免空白 500 页。  
- **`study.exception.expose-detail=true`** 时（仅建议开发环境），部分异常会在 `msg` 中带简要原因。生产请保持 **`false`**。  
- 管理端 Axios：HTTP ≥400 / 500 时若响应体含 **`msg`**，会优先提示该文案（见 `studyroom-vue/src/utils/httpRequest.js`）。

---

## 6. 审计日志（`@SysLog`）

- 切面 **`SysLogAspect`**：在标注 **`@SysLog("说明")`** 的接口上记录 `sys_log`（用户名优先取当前管理员帐号）。  
- 请勿对登录接口打 `@SysLog`，以免参数中包含密码。  
- 若表未创建，服务内会 **跳过落库** 并打 warn，不阻断业务。

---

## 7. 按钮级权限（`@RequiresPerm`）

- 拦截器在 **`SysAdminAuthInterceptor` 之后** 执行。  
- **`role_id = 1`（超级管理员）** 拥有全部接口权限。  
- 其它管理员：根据 **`sys_admin_role` → `sys_role_menu` → `sys_menu.perms`** 判断是否包含注解中的权限串（与菜单里 `perms` 一致，如 `bas:appointment:list`）。  
- **未标注** `@RequiresPerm` 的接口：**仍仅需登录**（便于渐进迁移）。

---

## 8. 导出示例

- **预约列表 CSV**：`GET /sys/bas/appointment/export`，与列表相同筛选条件，最多 **5000** 条。  
- 管理端页面：**预约管理** 提供「导出 CSV」按钮。

---

## 9. 预约单次最长与营业窗

- **单次连续预约**总时长上限<strong>不再单独配置</strong>，由 `study.reservation.day-start` / `day-end` 计算出的营业总分钟数决定（例如 08:00–22:30 为 870 分钟）；`TimeSlotCodec` 与询价/落库通过 `ReservationRuleConfigService#getMaxDurationMinutes()` 读取该值。  
- 时段仍须落在<strong>同一日历日</strong>，且遵守自习室开放时间（若有）与槽位粒度。  
- 表字段 `sys_reservation_rule.max_duration_minutes` 在保存规则时会被<strong>同步为当前营业跨度</strong>，便于报表或排查；**业务校验以 yml 为准**。  
- **提前预约天数**仍为 `advance_booking_days`（管理端可改），与「未来 N 天以内（含当日）」文案一致。

---

## 10. 预约调度（未签到 / 到期自动结束）

`application.yml` → `study.reservation`（须 **Redis 可用** 且 `scheduler-enabled: true`）：

| 配置项 | 默认 | 说明 |
|--------|------|------|
| `sign-in-grace-minutes` | 15 | 普通预约：开场后未签到，宽限期满自动释放 |
| `skip-no-show-for-store-auth` | true | **门店授权**预约：不因未签到提前释放，仅到期结束 |
| `auto-end-on-slot-finish` | true | 预约**结束时刻**：待签到→已取消，使用中→已完成并释座 |
| `scheduler-ms` | 10000 | 扫描间隔（毫秒） |

顾客端「入座」页约每 45 秒静默刷新进行中的预约；到点后状态会自动更新，无需手动签退（仍建议主动签退）。

---

## 11. 上线支付 / 短信 / 订阅消息

实现清单与对接步骤见：**`docs/PRODUCTION-INTEGRATIONS.md`**（与代码中的模拟支付、模拟短信并存；上线时替换为真实实现）。

---

## 12. 相关路径速查

| 路径 | 说明 |
|------|------|
| `studyroom-java/` | 后端 |
| `studyroom-vue/` | 管理端 |
| `studyroom-wx/` | 微信小程序 |
| `docs/PRODUCTION-INTEGRATIONS.md` | 微信支付、短信、订阅消息 |
